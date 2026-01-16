#if false 
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje MPI sinhronizacije i prekida (Subtasks 5.3.1, 5.3.2, 5.3.3)
    /// Prepravljeno da koristi MpiMining (IMpiCommunication).
    /// </summary>
    public class TestMpiSynchronization
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI sinhronizacije i prekida (MpiMining) ===\n");

            Console.WriteLine("Test 1: Broadcast STOP (5.3.1)");
            TestStopSignalDuringMining();
            Console.WriteLine();

            Console.WriteLine("Test 2: Bezbedno gašenje niti (5.3.2)");
            TestSafeThreadShutdown();
            Console.WriteLine();

            Console.WriteLine("Test 3: Cleanup (5.3.3)");
            TestMpiCleanup();
            Console.WriteLine();

            Console.WriteLine("✓ Testovi za 5.3.1–5.3.3 (MpiMining) su prošli!");
        }

        private static void TestStopSignalDuringMining()
        {
            SimulatedMpiCommunication.ClearQueue();

            int worldSize = 4;
            int threadsPerWorker = 2;

            // PLACEHOLDER: comm za master
            IMpiCommunication masterComm = MakeSimComm(rank: 0, size: worldSize);

            // Start worker-i
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

            // Master pokreće mining (na kraju će Broadcast STOP)
            var template = new Block(
                index: 1,
                data: "Stop signal test",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 2, // spusti na 1 ako je sporo
                nonce: 0
            );

            var masterTask = Task.Run(() =>
            {
                return MpiMining.RunDistributedMiningAsMaster(
                    comm: masterComm,
                    worldSize: worldSize,
                    blockTemplate: template,
                    threadsPerWorker: threadsPerWorker
                );
            });

            // Ako sve radi, master + worker-i moraju završiti u timeout-u
            if (!masterTask.Wait(20_000))
                throw new Exception("FAIL: Master mining nije završio (možda difficulty previsok).");

            if (!Task.WaitAll(workerTasks.ToArray(), 20_000))
                throw new Exception("FAIL: Neki worker nije završio (Broadcast STOP problem).");

            var mined = masterTask.Result;
            Console.WriteLine($"  ✓ Master završio i poslao STOP svima. Hash={mined.Hash?.Substring(0, Math.Min(20, mined.Hash.Length))}...");
        }

        private static void TestSafeThreadShutdown()
        {
            Console.WriteLine("  Testiranje bezbednog gašenja niti...");

            int numThreads = 4;
            var pool = new ThreadedMiner.MiningThreadPool(numThreads);
            var sharedState = pool.SharedState;

            var workers = new List<ThreadedMiner.MiningWorker>();
            var blockToMine = new Block(
                index: 1,
                data: "Test block for safe shutdown",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 3,
                nonce: 0
            );

            for (int i = 0; i < numThreads; i++)
            {
                var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(numThreads, i);

                var blockCopy = new Block(
                    blockToMine.Index,
                    blockToMine.Data,
                    blockToMine.Timestamp,
                    blockToMine.PreviousHash,
                    blockToMine.Difficulty,
                    blockToMine.Nonce
                );

                var worker = new ThreadedMiner.BlockMiningWorker(
                    threadId: i,
                    sharedState: sharedState,
                    blockToMine: blockCopy,
                    startNonce: startNonce,
                    endNonce: Math.Min(startNonce + 1_000_000, endNonce)
                );

                workers.Add(worker);
            }

            Console.WriteLine($"  Pokretanje {numThreads} niti za mining...");
            pool.StartWithWorkers(workers);

            Thread.Sleep(200);

            if (pool.IsRunning)
                Console.WriteLine($"  ✓ ThreadPool je pokrenut ({pool.ActiveThreadCount}/{numThreads} niti aktivno)");

            Console.WriteLine("  Zaustavljam ThreadPool bezbedno...");
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();
            pool.Stop();
            stopwatch.Stop();

            if (!pool.AllThreadsCompleted)
                throw new Exception($"FAIL: Neke niti još rade (ActiveThreadCount: {pool.ActiveThreadCount})");

            Console.WriteLine($"  ✓ Sve niti su bezbedno zaustavljene, vreme: {stopwatch.ElapsedMilliseconds} ms");

            Console.WriteLine("  Test 2: Brzo zaustavljanje...");
            pool = new ThreadedMiner.MiningThreadPool(2);
            sharedState = pool.SharedState;

            var quickWorkers = new List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < 2; i++)
            {
                var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(2, i);

                var blockCopy = new Block(
                    blockToMine.Index,
                    blockToMine.Data,
                    blockToMine.Timestamp,
                    blockToMine.PreviousHash,
                    blockToMine.Difficulty,
                    blockToMine.Nonce
                );

                quickWorkers.Add(new ThreadedMiner.BlockMiningWorker(i, sharedState, blockCopy, startNonce, endNonce));
            }

            pool.StartWithWorkers(quickWorkers);
            Thread.Sleep(100);
            pool.Stop();

            if (!pool.AllThreadsCompleted)
                throw new Exception("FAIL: Neke niti još rade nakon brzog zaustavljanja.");

            Console.WriteLine("  ✓ Brzo zaustavljanje uspešno.");
        }

        private static void TestMpiCleanup()
        {
            Console.WriteLine("  Cleanup: čišćenje simulirane MPI komunikacije...");

            SimulatedMpiCommunication.ClearQueue();

            Console.WriteLine("  ✓ Queue-evi očišćeni.");
            // U MpiMining nema posebnog cleanup API-ja kao ranije (MpiCleanup),
            // jer se oslanja na IMpiCommunication + Broadcast STOP.
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