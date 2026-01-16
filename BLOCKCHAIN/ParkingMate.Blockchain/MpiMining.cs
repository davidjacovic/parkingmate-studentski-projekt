using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;

namespace ParkingMate.Blockchain
{
    public static class MpiMining
    {
        public const int TAG_NONCE_RANGE = 1;
        public const int TAG_RESULT = 2;
        public const int TAG_STOP = 3;

        [Serializable]
        public class NonceRangeMessage
        {
            public bool IsShutdown { get; set; }
            public ulong StartNonce { get; set; }
            public ulong EndNonce { get; set; }
            public Block BlockToMine { get; set; } = new Block();
        }


        [Serializable]
        public class MiningResultMessage
        {
            public bool Success { get; set; }
            public Block? FoundBlock { get; set; }
            public int WorkerRank { get; set; }
            public long MiningTimeMs { get; set; }
        }

        [Serializable]
        public class StopMessage
        {
            public bool Stop { get; set; }
        }

        public static Block RunDistributedMiningAsMaster(
     IMpiCommunication comm,
     int worldSize,
     Block blockTemplate,
     int threadsPerWorker)
        {
            if (comm == null) throw new ArgumentNullException(nameof(comm));
            if (blockTemplate == null) throw new ArgumentNullException(nameof(blockTemplate));

            if (worldSize < 2)
                throw new InvalidOperationException("Za MPI mining treba bar 2 procesa (1 master + 1 worker).");

            int numWorkers = worldSize - 1;

            // 1) podeli nonce prostor po workerima
            var ranges = new List<(ulong start, ulong end)>(numWorkers);
            for (int i = 0; i < numWorkers; i++)
                ranges.Add(ThreadedMiner.CalculateNonceRange(numWorkers, i));

            Console.WriteLine($"[MASTER] distributing work to {numWorkers} workers (worldSize={worldSize})...");
            Console.Out.Flush();

            // 2) posalji posao svim workerima
            for (int i = 0; i < numWorkers; i++)
            {
                int workerRank = i + 1;
                var (start, end) = ranges[i];

                var msg = new NonceRangeMessage
                {
                    StartNonce = start,
                    EndNonce = end,
                    BlockToMine = new Block(
                        blockTemplate.Index,
                        blockTemplate.Data,
                        blockTemplate.Timestamp,
                        blockTemplate.PreviousHash,
                        blockTemplate.Difficulty,
                        nonce: 0
                    )
                };

                Console.WriteLine($"[MASTER] send job -> rank {workerRank} range=[{start}..{end}] diff={msg.BlockToMine.Difficulty}");
                Console.Out.Flush();

                comm.Send(msg, workerRank, TAG_NONCE_RANGE);
            }

            // 3) primi TACNO numWorkers rezultata (da ne ostanu stari u queue-u)
            MiningResultMessage? winner = null;

            for (int k = 0; k < numWorkers; k++)
            {
                var res = comm.ReceiveAnySource<MiningResultMessage>(TAG_RESULT, out int src);

                Console.WriteLine($"[MASTER] got result from src={src} workerRank={res.WorkerRank} success={res.Success} time={res.MiningTimeMs}ms");
                Console.Out.Flush();

                // izaberi prvog koji je uspeo
                if (winner == null && res.Success && res.FoundBlock != null)
                    winner = res;
            }

            // 4) STOP svima (uvek)
            var stop = new StopMessage { Stop = true };
            for (int r = 1; r <= numWorkers; r++)
                comm.Send(stop, r, TAG_STOP);

            if (winner == null || winner.FoundBlock == null)
                throw new InvalidOperationException("Nijedan worker nije našao validan blok u ovoj rundi.");

            Console.WriteLine($"[MASTER] Found by rank {winner.WorkerRank} in {winner.MiningTimeMs} ms");
            Console.Out.Flush();

            return winner.FoundBlock;
        }

        public static void SendShutdownToWorkers(IMpiCommunication comm, int worldSize)
        {
            int numWorkers = worldSize - 1;

            var shutdown = new NonceRangeMessage
            {
                IsShutdown = true,
                StartNonce = 0,
                EndNonce = 0,
                BlockToMine = new Block() // mora nešto zbog serijalizacije
            };

            for (int r = 1; r <= numWorkers; r++)
                comm.Send(shutdown, r, TAG_NONCE_RANGE);
        }


        public static bool RunAsWorker(IMpiCommunication comm, int myRank, int threadsPerWorker)
        {
            try
            {
                Console.WriteLine($"[WORKER {myRank}] waiting for job...");
                Console.Out.Flush();

                var job = comm.Receive<NonceRangeMessage>(sourceRank: 0, tag: TAG_NONCE_RANGE);

                if (job.IsShutdown)
                {
                    Console.WriteLine($"[WORKER {myRank}] got SHUTDOWN job. exiting.");
                    Console.Out.Flush();
                    return true;
                }

                Console.WriteLine($"[WORKER {myRank}] got job [{job.StartNonce}..{job.EndNonce}] diff={job.BlockToMine.Difficulty}");
                Console.Out.Flush();

                var pool = new ThreadedMiner.MiningThreadPool(threadsPerWorker);
                var shared = pool.SharedState;

                var workers = new List<ThreadedMiner.MiningWorker>(threadsPerWorker);

                ulong rangeSize = (job.EndNonce >= job.StartNonce) ? (job.EndNonce - job.StartNonce + 1) : 0;
                ulong perThread = rangeSize / (ulong)threadsPerWorker;
                if (perThread == 0) perThread = 1;

                for (int t = 0; t < threadsPerWorker; t++)
                {
                    ulong start = job.StartNonce + (ulong)t * perThread;

                    ulong end;
                    if (t == threadsPerWorker - 1) end = job.EndNonce;
                    else
                    {
                        ulong tentativeEnd = start + perThread - 1;
                        end = (tentativeEnd > job.EndNonce) ? job.EndNonce : tentativeEnd;
                    }

                    var copy = new Block(
                        job.BlockToMine.Index,
                        job.BlockToMine.Data,
                        job.BlockToMine.Timestamp,
                        job.BlockToMine.PreviousHash,
                        job.BlockToMine.Difficulty,
                        0
                    );

                    workers.Add(new ThreadedMiner.BlockMiningWorker(t, shared, copy, start, end));
                }

                var sw = Stopwatch.StartNew();
                pool.StartWithWorkers(workers);
                pool.WaitAll();
                sw.Stop();

                var found = shared.GetFoundBlock();

                Console.WriteLine($"[WORKER {myRank}] mining done success={(found != null)} in {sw.ElapsedMilliseconds}ms, sending result...");
                Console.Out.Flush();

                // ✅ UVEK šalji rezultat (success ili fail) da master ne visi
                var result = new MiningResultMessage
                {
                    WorkerRank = myRank,
                    MiningTimeMs = sw.ElapsedMilliseconds,
                    Success = (found != null),
                    FoundBlock = found
                };
                comm.Send(result, destinationRank: 0, tag: TAG_RESULT);

                Console.WriteLine($"[WORKER {myRank}] waiting STOP...");
                Console.Out.Flush();

                var stopMsg = comm.Receive<StopMessage>(sourceRank: 0, tag: TAG_STOP);

                Console.WriteLine($"[WORKER {myRank}] got STOP={stopMsg.Stop}, returning to wait next job.");
                Console.Out.Flush();

                pool.Stop();
                return false; // nije shutdown
            }
            catch (Exception ex)
            {
                Console.WriteLine($"[WORKER {myRank}] FATAL: {ex}");
                Console.Out.Flush();

                // Probaj da pošalješ fail masteru da master ne visi zauvek
                try
                {
                    comm.Send(new MiningResultMessage
                    {
                        WorkerRank = myRank,
                        MiningTimeMs = -1,
                        Success = false,
                        FoundBlock = null
                    }, destinationRank: 0, tag: TAG_RESULT);
                }
                catch { /* ignore */ }

                // ako pukne, izađi iz petlje da se proces završi
                return true;
            }
        }
    }
}
