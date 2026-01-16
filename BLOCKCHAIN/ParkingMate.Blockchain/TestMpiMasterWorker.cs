#if false 
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje MPI Master-Worker arhitekture (Subtasks 5.2.1, 5.2.2, 5.2.3, 5.2.4, 5.2.5)
    /// Prepravljeno da koristi MpiMining (IMpiCommunication).
    /// </summary>
    public class TestMpiMasterWorker
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI Master-Worker arhitekture (MpiMining) ===\n");

            Console.WriteLine("Test 1: Master generiše nonce opsege (5.2.1)");
            TestMasterGenerateRanges();
            Console.WriteLine();

            Console.WriteLine("Test 2: Slanje nonce opsega worker procesima (5.2.2)");
            TestSendRangesToWorkers();
            Console.WriteLine();

            Console.WriteLine("Test 3: Worker pokreće multi-thread PoW (5.2.3) + šalje rezultat (5.2.4)");
            TestWorkerMultiThreadMiningAndResult();
            Console.WriteLine();

            Console.WriteLine("Test 4: Master broadcast STOP svima (5.2.5) (integracioni)");
            TestMasterNotifiesWorkersStopBroadcast();
            Console.WriteLine();

            Console.WriteLine("✓ Testovi za 5.2.1–5.2.5 (MpiMining) su prošli!");
        }

        private static void TestMasterGenerateRanges()
        {
            int worldSize = 4;
            int numWorkers = worldSize - 1;

            // MpiMining koristi ThreadedMiner.CalculateNonceRange(numWorkers, i)
            var ranges = new List<(ulong start, ulong end)>();
            for (int i = 0; i < numWorkers; i++)
                ranges.Add(ThreadedMiner.CalculateNonceRange(numWorkers, i));

            if (ranges.Count != 3)
                throw new Exception($"FAIL: Očekivano 3 opsega, dobijeno {ranges.Count}");

            Console.WriteLine($"  ✓ Generisano {ranges.Count} opsega za {numWorkers} worker-a");

            // Provera: bez preklapanja (dozvoljeno start == prevEnd ako su “uzastopni” po tvojoj definiciji)
            bool noOverlap = true;
            ulong? prevEnd = null;

            for (int i = 0; i < ranges.Count; i++)
            {
                var (start, end) = ranges[i];
                int workerRank = i + 1;

                Console.WriteLine($"    Worker {workerRank}: [{start:N0}, {end:N0}]");

                if (prevEnd.HasValue && start < prevEnd.Value)
                {
                    noOverlap = false;
                    Console.WriteLine($"      ✗ Preklapanje: start({start:N0}) < prevEnd({prevEnd.Value:N0})");
                }

                prevEnd = end;
            }

            if (!noOverlap)
                throw new Exception("FAIL: Opsezi se preklapaju (ne bi trebalo).");

            Console.WriteLine("  ✓ Opsezi su bez preklapanja.");
        }

        private static void TestSendRangesToWorkers()
        {
            SimulatedMpiCommunication.ClearQueue();

            int worldSize = 4;
            int numWorkers = worldSize - 1;

            // PLACEHOLDER: napravi IMpiCommunication za master rank 0
            // ZAMENI OVU LINIJU prema tvojoj implementaciji:
            IMpiCommunication masterComm = MakeSimComm(rank: 0, size: worldSize);

            var blockTemplate = new Block(
                index: 1,
                data: "Test send ranges",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 2,
                nonce: 0
            );

            // Master ručno šalje poruke kao što radi MpiMining.RunDistributedMiningAsMaster
            for (int i = 0; i < numWorkers; i++)
            {
                int workerRank = i + 1;
                var (start, end) = ThreadedMiner.CalculateNonceRange(numWorkers, i);

                var msg = new MpiMining.NonceRangeMessage
                {
                    StartNonce = start,
                    EndNonce = end,
                    BlockToMine = new Block(
                        blockTemplate.Index,
                        blockTemplate.Data,
                        blockTemplate.Timestamp,
                        blockTemplate.PreviousHash,
                        blockTemplate.Difficulty,
                        0
                    )
                };

                masterComm.Send(msg, workerRank, MpiMining.TAG_NONCE_RANGE);
            }

            // Svaki worker treba da može da primi poruku od mastera (rank 0)
            for (int workerRank = 1; workerRank < worldSize; workerRank++)
            {
                // PLACEHOLDER: napravi IMpiCommunication za workerRank
                IMpiCommunication workerComm = MakeSimComm(rank: workerRank, size: worldSize);

                var job = workerComm.Receive<MpiMining.NonceRangeMessage>(sourceRank: 0, tag: MpiMining.TAG_NONCE_RANGE);
                if (job == null)
                    throw new Exception($"FAIL: Worker {workerRank} nije primio NonceRangeMessage.");

                Console.WriteLine($"  ✓ Worker {workerRank} primio job: [{job.StartNonce:N0}, {job.EndNonce:N0}]");
            }

            Console.WriteLine("  ✓ Svi worker-i su primili nonce opsege.");
        }

        private static void TestWorkerMultiThreadMiningAndResult()
        {
            SimulatedMpiCommunication.ClearQueue();

            int worldSize = 2;      // dovoljno za ovaj test: 1 master + 1 worker
            int workerRank = 1;
            int threadsPerWorker = 2;

            // PLACEHOLDER: comm za master i worker
            IMpiCommunication masterComm = MakeSimComm(rank: 0, size: worldSize);
            IMpiCommunication workerComm = MakeSimComm(rank: workerRank, size: worldSize);

            // Master pošalje mali range + nižu težinu da test ne traje dugo
            var job = new MpiMining.NonceRangeMessage
            {
                StartNonce = 0,
                EndNonce = 200_000, // mali opseg za test
                BlockToMine = new Block(
                    index: 1,
                    data: "Worker mining test",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "0",
                    difficulty: 2, // spusti na 1 ako je sporo
                    nonce: 0
                )
            };

            masterComm.Send(job, workerRank, MpiMining.TAG_NONCE_RANGE);

            // Worker radi MpiMining.RunAsWorker u task-u (da bismo istovremeno čekali rezultat)
            var t = Task.Run(() =>
            {
                MpiMining.RunAsWorker(workerComm, myRank: workerRank, threadsPerWorker: threadsPerWorker);
            });

            // Master primi rezultat
            var res = masterComm.Receive<MpiMining.MiningResultMessage>(sourceRank: workerRank, tag: MpiMining.TAG_RESULT);

            if (res == null)
                throw new Exception("FAIL: Master nije primio MiningResultMessage.");

            Console.WriteLine($"  ✓ Master primio rezultat od rank {res.WorkerRank}: success={res.Success}, time={res.MiningTimeMs}ms");

            // Master mora da broadcast STOP (u RunDistributedMiningAsMaster bi to uradio; ovde ručno)
            var stop = new MpiMining.StopMessage { Stop = true };
            masterComm.Broadcast(ref stop, rootRank: 0);

            // Worker čeka stop broadcast pre izlaska, pa task mora da se završi
            if (!t.Wait(10_000))
                throw new Exception("FAIL: Worker nije završio (verovatno nije primio STOP broadcast).");

            if (res.Success && res.FoundBlock != null)
            {
                string prefix = new string('0', (int)res.FoundBlock.Difficulty);

                if (res.FoundBlock.Hash == null || !res.FoundBlock.Hash.StartsWith(prefix))
                    throw new Exception("FAIL: FoundBlock hash ne zadovoljava difficulty prefix.");

                if (res.FoundBlock.CalculateHash() != res.FoundBlock.Hash)
                    throw new Exception("FAIL: FoundBlock.Hash != FoundBlock.CalculateHash().");

                Console.WriteLine($"  ✓ FoundBlock validan. Nonce={res.FoundBlock.Nonce:N0}, Hash={res.FoundBlock.Hash.Substring(0, Math.Min(20, res.FoundBlock.Hash.Length))}...");
            }
            else
            {
                Console.WriteLine("  ⚠ Worker nije našao rešenje u malom opsegu (nije nužno greška).");
                Console.WriteLine("    Ako želiš da ovaj test bude deterministički: povećaj EndNonce ili spusti difficulty na 1.");
            }
        }

        private static void TestMasterNotifiesWorkersStopBroadcast()
        {
            SimulatedMpiCommunication.ClearQueue();

            int worldSize = 4;
            int threadsPerWorker = 2;

            // PLACEHOLDER: comm za master + comm za sve workere
            IMpiCommunication masterComm = MakeSimComm(rank: 0, size: worldSize);

            var workerTasks = new List<Task>();
            for (int rank = 1; rank < worldSize; rank++)
            {
                int r = rank;
                IMpiCommunication workerComm = MakeSimComm(rank: r, size: worldSize);

                workerTasks.Add(Task.Run(() =>
                {
                    MpiMining.RunAsWorker(workerComm, myRank: r, threadsPerWorker: threadsPerWorker);
                }));
            }

            var template = new Block(
                index: 1,
                data: "Stop broadcast integration test",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 2, // spusti ako je sporo
                nonce: 0
            );

            // Ovo internally: šalje poslove, primi rezultate, onda Broadcast STOP svima
            var mined = MpiMining.RunDistributedMiningAsMaster(
                comm: masterComm,
                worldSize: worldSize,
                blockTemplate: template,
                threadsPerWorker: threadsPerWorker
            );

            if (mined == null || mined.Hash == null)
                throw new Exception("FAIL: Master nije dobio mined blok.");

            // Worker-i moraju da završe jer čekaju broadcast STOP
            if (!Task.WaitAll(workerTasks.ToArray(), 15_000))
                throw new Exception("FAIL: Neki worker nije završio (STOP broadcast nije “prošao”).");

            Console.WriteLine("  ✓ Svi worker-i su završili nakon STOP broadcast-a.");
        }

        private static IMpiCommunication MakeSimComm(int rank, int size)
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();
            mpi.Initialize(size: size, rank: rank);
            return new SimulatedMpiCommunication(mpi);
        }

    }

}
#endif