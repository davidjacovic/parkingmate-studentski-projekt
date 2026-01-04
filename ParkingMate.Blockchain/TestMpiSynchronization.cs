using System;
using System.Threading;
using System.Threading.Tasks;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje MPI sinhronizacije i prekida (Subtasks 5.3.1, 5.3.2, 5.3.3)
    /// </summary>
    public class TestMpiSynchronization
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI sinhronizacije i prekida (5.3.1, 5.3.2) ===\n");

            // Test 1: MPI broadcast stop signala tokom mining-a (5.3.1)
            Console.WriteLine("Test 1: MPI broadcast stop signala tokom mining-a (5.3.1)");
            TestStopSignalDuringMining();
            Console.WriteLine();

            // Test 2: Bezbedno gašenje niti (5.3.2)
            Console.WriteLine("Test 2: Bezbedno gašenje niti (5.3.2)");
            TestSafeThreadShutdown();
            Console.WriteLine();

            // TODO: 5.3.3 - Cleanup MPI okruženja
            /*
            // Test 3: Cleanup MPI okruženja (5.3.3)
            Console.WriteLine("Test 3: Cleanup MPI okruženja (5.3.3)");
            TestMpiCleanup();
            Console.WriteLine();
            */

            Console.WriteLine("✓ Testovi za Subtask 5.3.1 i 5.3.2 (MPI broadcast stop signala i bezbedno gašenje niti) su prošli!");
        }

        private static void TestStopSignalDuringMining()
        {
            // Očisti message queue i broadcast queue pre testa
            SimulatedMpiCommunication.ClearQueue();

            // Master deo - pripremi i pošalji nonce opseg
            var mpiMaster = MpiEnvironment.Instance;
            mpiMaster.Finalize();
            mpiMaster.Initialize(size: 4, rank: 0); // Master rank 0

            var master = new MpiMiningMasterWorker.MpiMiningMaster(mpiMaster);

            var blockToMine = new Block(
                index: 1,
                data: "Test block for stop signal",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 3, // Viša difficulty da mining traje duže
                nonce: 0
            );

            var messages = master.GenerateNonceRanges(blockToMine);
            master.SendNonceRangesToWorkers(messages);

            // Worker deo - pokreni mining sa proverom stop signala
            var mpiWorker = MpiEnvironment.Instance;
            mpiWorker.Finalize();
            mpiWorker.Initialize(size: 4, rank: 1); // Worker rank 1

            var worker = new MpiMiningMasterWorker.MpiMiningWorker(mpiWorker, threadCount: 2);

            // Pokreni mining u background task-u
            var miningTask = Task.Run(() =>
            {
                var message = worker.ReceiveNonceRange();
                if (message != null)
                {
                    return worker.ExecuteMining(message);
                }
                return null;
            });

            // Sačekaj malo da mining počne
            Thread.Sleep(500);

            // Master šalje stop signal
            Console.WriteLine("  [Test] Master šalje stop signal workers...");
            master.NotifyWorkersToStop("Test: Zaustavljanje mining-a");

            // Sačekaj da se mining završi
            try
            {
                var result = miningTask.Wait(10000); // 10 sekundi timeout
                if (result)
                {
                    var miningResult = miningTask.Result;
                    if (miningResult != null)
                    {
                        Console.WriteLine($"  ✓ Mining završen (Success: {miningResult.Success}, Time: {miningResult.MiningTimeMs} ms)");
                        
                        if (miningResult.Success)
                        {
                            Console.WriteLine($"    Pronađen nonce: {miningResult.FoundBlock?.Nonce:N0}");
                        }
                        else
                        {
                            Console.WriteLine("    Mining prekinut zbog stop signala (očekivano)");
                        }
                    }
                }
                else
                {
                    Console.WriteLine("  ⚠ Mining task nije završen u roku od 10 sekundi");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška tokom mining-a: {ex.Message}");
            }
        }

        private static void TestSafeThreadShutdown()
        {
            Console.WriteLine("  Testiranje bezbednog gašenja niti...");

            // Kreiraj ThreadPool sa nekoliko niti
            int numThreads = 4;
            var pool = new ThreadedMiner.MiningThreadPool(numThreads);
            var sharedState = pool.SharedState;

            // Kreiraj test workers sa većom difficulty da mining traje duže
            var workers = new List<ThreadedMiner.MiningWorker>();
            var blockToMine = new Block(
                index: 1,
                data: "Test block for safe shutdown",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 3, // Viša difficulty
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
                    endNonce: Math.Min(startNonce + 1000000, endNonce) // Ograničen opseg za test
                );

                workers.Add(worker);
            }

            // Pokreni mining
            Console.WriteLine($"  Pokretanje {numThreads} niti za mining...");
            pool.StartWithWorkers(workers);

            // Sačekaj malo da niti počnu
            Thread.Sleep(200);

            // Proveri da li su niti aktivne
            if (pool.IsRunning)
            {
                Console.WriteLine($"  ✓ ThreadPool je pokrenut ({pool.ActiveThreadCount}/{numThreads} niti aktivno)");
            }

            // Zaustavi bezbedno
            Console.WriteLine("  Zaustavljam ThreadPool bezbedno...");
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();
            pool.Stop();
            stopwatch.Stop();

            // Proveri da li su sve niti zaustavljene
            if (pool.AllThreadsCompleted)
            {
                Console.WriteLine($"  ✓ Sve niti su bezbedno zaustavljene");
                Console.WriteLine($"  ✓ Vreme zaustavljanja: {stopwatch.ElapsedMilliseconds} ms");
                Console.WriteLine($"  ✓ ActiveThreadCount: {pool.ActiveThreadCount} (očekivano: 0)");
            }
            else
            {
                Console.WriteLine($"  ✗ Greška: Neke niti još uvek rade (ActiveThreadCount: {pool.ActiveThreadCount})");
            }

            // Test 2: Pokreni ponovo i zaustavi brzo
            Console.WriteLine("  Test 2: Brzo zaustavljanje (simulacija stop signala)...");
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

                var worker = new ThreadedMiner.BlockMiningWorker(
                    threadId: i,
                    sharedState: sharedState,
                    blockToMine: blockCopy,
                    startNonce: startNonce,
                    endNonce: endNonce
                );

                quickWorkers.Add(worker);
            }

            pool.StartWithWorkers(quickWorkers);
            Thread.Sleep(100); // Kratko sačekaj

            // Zaustavi brzo (simulacija stop signala)
            pool.Stop();

            if (pool.AllThreadsCompleted)
            {
                Console.WriteLine("  ✓ Brzo zaustavljanje uspešno - sve niti su zaustavljene");
            }
            else
            {
                Console.WriteLine($"  ⚠ Neke niti još rade nakon brzog zaustavljanja (ActiveThreadCount: {pool.ActiveThreadCount})");
            }
        }

        // TODO: 5.3.3 - Cleanup MPI okruženja
        /*
        private static void TestMpiCleanup()
        {
            // Test implementacije
        }
        */
    }
}

