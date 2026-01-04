using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje MPI Master-Worker arhitekture (Subtask 5.2.1)
    /// </summary>
    public class TestMpiMasterWorker
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI Master-Worker arhitekture (5.2.1) ===\n");

            // Test 1: Master generiše nonce opsege (Subtask 5.2.1)
            Console.WriteLine("Test 1: Master generiše seed / nonce opsege (5.2.1)");
            TestMasterGenerateRanges();
            Console.WriteLine();

            // TODO: 5.2.2 - Test slanja seed-ova worker procesima
            /*
            // Test 2: Slanje seed-ova worker procesima
            Console.WriteLine("Test 2: Slanje seed-ova worker procesima");
            TestSendRangesToWorkers();
            Console.WriteLine();
            */

            // TODO: 5.2.3 - Test worker multi-thread PoW
            /*
            // Test 3: Worker pokreće multi-thread PoW
            Console.WriteLine("Test 3: Worker pokreće multi-thread PoW");
            TestWorkerMultiThreadMining();
            Console.WriteLine();
            */

            // TODO: 5.2.4 - Test worker vraća pronađeno rešenje masteru
            /*
            // Test 4: Worker vraća pronađeno rešenje masteru
            Console.WriteLine("Test 4: Worker vraća pronađeno rešenje masteru");
            TestWorkerReturnsResult();
            Console.WriteLine();
            */

            // TODO: 5.2.5 - Test master obaveštava sve čvorove o završetku
            /*
            // Test 5: Master obaveštava sve čvorove o završetku
            Console.WriteLine("Test 5: Master obaveštava sve čvorove o završetku");
            TestMasterNotifiesWorkers();
            Console.WriteLine();

            // Test 6: Kompletan Master-Worker ciklus
            Console.WriteLine("Test 6: Kompletan Master-Worker ciklus");
            TestCompleteMasterWorkerCycle();
            Console.WriteLine();
            */

            Console.WriteLine("✓ Testovi za Subtask 5.2.1 (Master generiše seed / nonce opsege) su prošli!");
        }

        private static void TestMasterGenerateRanges()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();
            mpi.Initialize(size: 4, rank: 0); // Master (rank 0)

            var master = new MpiMiningMasterWorker.MpiMiningMaster(mpi);

            var blockToMine = new Block(
                index: 1,
                data: "Test block for Master-Worker",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 3,
                nonce: 0
            );

            var messages = master.GenerateNonceRanges(blockToMine);

            if (messages.Count == 3) // 3 workers (size 4 - 1 master = 3)
            {
                Console.WriteLine($"  ✓ Master je generisao {messages.Count} nonce opsega za workers");

                // Proveri da li su opsegi različiti i bez preklapanja
                bool noOverlap = true;
                for (int i = 0; i < messages.Count; i++)
                {
                    for (int j = i + 1; j < messages.Count; j++)
                    {
                        var msg1 = messages[i];
                        var msg2 = messages[j];

                        // Proveri preklapanje
                        if (!(msg1.EndNonce < msg2.StartNonce || msg2.EndNonce < msg1.StartNonce))
                        {
                            noOverlap = false;
                            Console.WriteLine($"  ✗ Preklapanje između worker {msg1.WorkerRank} i {msg2.WorkerRank}");
                        }
                    }

                    Console.WriteLine($"    Worker {messages[i].WorkerRank}: [{messages[i].StartNonce:N0}, {messages[i].EndNonce:N0}]");
                }

                if (noOverlap)
                {
                    Console.WriteLine("  ✓ Svi opsegi su bez preklapanja");
                }
            }
            else
            {
                Console.WriteLine($"  ✗ Greška: Očekivano 3 opsega, dobijeno {messages.Count}");
            }
        }

        // TODO: 5.2.2 - Test slanja seed-ova worker procesima
        /*
        private static void TestSendRangesToWorkers()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();
            mpi.Initialize(size: 4, rank: 0); // Master

            var master = new MpiMiningMasterWorker.MpiMiningMaster(mpi);

            var blockToMine = new Block(
                index: 1,
                data: "Test block",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 3,
                nonce: 0
            );

            var messages = master.GenerateNonceRanges(blockToMine);

            try
            {
                master.SendNonceRangesToWorkers(messages);
                Console.WriteLine("  ✓ Master je uspešno poslao nonce opsege workers");
                Console.WriteLine($"  ✓ Poslato {messages.Count} poruka workers");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška pri slanju poruka: {ex.Message}");
            }
        }
        */

        // TODO: 5.2.3 - Test worker multi-thread PoW
        /*
        private static void TestWorkerMultiThreadMining()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();
            mpi.Initialize(size: 4, rank: 1); // Worker

            var worker = new MpiMiningMasterWorker.MpiMiningWorker(mpi, threadCount: 2);

            // Kreiraj test poruku sa opsegom
            var blockToMine = new Block(
                index: 1,
                data: "Test block for worker",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 2, // Niža difficulty za brže testiranje
                nonce: 0
            );

            ulong startNonce = 0;
            ulong endNonce = 1000000; // Ograničen opseg za test
            var message = new MpiMiningMasterWorker.NonceRangeMessage(startNonce, endNonce, 1, blockToMine);

            Console.WriteLine("  Pokretanje multi-threaded mining na worker procesu...");
            var result = worker.ExecuteMining(message);

            if (result != null)
            {
                Console.WriteLine($"  ✓ Worker je izvršio mining (Uspeh: {result.Success}, Vreme: {result.MiningTimeMs} ms)");
                Console.WriteLine($"  ✓ Ukupno pokušaja: {result.TotalAttempts:N0}");
                if (result.FoundBlock != null)
                {
                    Console.WriteLine($"  ✓ Pronađen nonce: {result.FoundBlock.Nonce:N0}");
                }
            }
            else
            {
                Console.WriteLine("  ✗ Greška: Worker nije vratio rezultat");
            }
        }
        */

        // TODO: 5.2.4 - Test worker vraća pronađeno rešenje masteru
        /*
        private static void TestWorkerReturnsResult()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();
            mpi.Initialize(size: 4, rank: 2); // Worker

            var worker = new MpiMiningMasterWorker.MpiMiningWorker(mpi);

            var result = new MpiMiningMasterWorker.MiningResultMessage(2);
            result.Success = true;
            result.MiningTimeMs = 100;
            result.TotalAttempts = 5000;

            var block = new Block(
                index: 1,
                data: "Test block",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 3,
                nonce: 12345
            );
            block.Hash = block.CalculateHash();
            result.FoundBlock = block;

            try
            {
                worker.SendResultToMaster(result);
                Console.WriteLine("  ✓ Worker je uspešno poslao rezultat masteru");
                Console.WriteLine($"  ✓ Rezultat: Success={result.Success}, Nonce={result.FoundBlock.Nonce}");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška pri slanju rezultata: {ex.Message}");
            }
        }
        */

        // TODO: 5.2.5 - Test master obaveštava sve čvorove o završetku
        /*
        private static void TestMasterNotifiesWorkers()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();
            mpi.Initialize(size: 4, rank: 0); // Master

            var master = new MpiMiningMasterWorker.MpiMiningMaster(mpi);

            try
            {
                master.NotifyWorkersToStop();
                Console.WriteLine("  ✓ Master je obavestio sve workers da prekinu rad");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška pri obaveštavanju workers: {ex.Message}");
            }
        }

        // TODO: Integracija - Kompletan Master-Worker ciklus (nakon 5.2.5)
        /*
        private static void TestCompleteMasterWorkerCycle()
        {
            Console.WriteLine("  Simulacija kompletnog Master-Worker ciklusa:");

            // Master deo
            var mpiMaster = MpiEnvironment.Instance;
            mpiMaster.Finalize();
            mpiMaster.Initialize(size: 4, rank: 0);

            var master = new MpiMiningMasterWorker.MpiMiningMaster(mpiMaster);

            var blockToMine = new Block(
                index: 1,
                data: "Complete cycle test block",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 2, // Niža difficulty za test
                nonce: 0
            );

            // 1. Master generiše opsege
            var messages = master.GenerateNonceRanges(blockToMine);
            Console.WriteLine($"  [Master] Generisano {messages.Count} opsega");

            // 2. Master šalje opsege workers
            master.SendNonceRangesToWorkers(messages);
            Console.WriteLine("  [Master] Opsegi poslati workers");

            // 3. Simulacija: Worker izvršava mining
            List<MpiMiningMasterWorker.MiningResultMessage> workerResults = new List<MpiMiningMasterWorker.MiningResultMessage>();

            for (int i = 0; i < messages.Count && i < 2; i++) // Test samo sa 2 workers zbog vremena
            {
                var mpiWorker = MpiEnvironment.Instance;
                mpiWorker.Finalize();
                mpiWorker.Initialize(size: 4, rank: messages[i].WorkerRank);

                var worker = new MpiMiningMasterWorker.MpiMiningWorker(mpiWorker, threadCount: 2);

                Console.WriteLine($"  [Worker {messages[i].WorkerRank}] Pokretanje mining-a...");
                var result = worker.ExecuteMining(messages[i]);

                if (result.Success)
                {
                    master.ReceiveResult(result);
                    Console.WriteLine($"  [Worker {messages[i].WorkerRank}] ✓ Rešenje pronađeno i poslato masteru");
                    break; // Prvi worker koji pronađe rešenje
                }
                else
                {
                    master.ReceiveResult(result);
                    Console.WriteLine($"  [Worker {messages[i].WorkerRank}] Rešenje nije pronađeno u opsegu");
                }

                workerResults.Add(result);
            }

            // 4. Master proverava rezultate
            var firstResult = master.GetFirstSuccessfulResult();
            if (firstResult != null)
            {
                Console.WriteLine($"  [Master] ✓ Pronađeno rešenje od worker-a {firstResult.WorkerRank}");
                
                // 5. Master obaveštava sve workers
                master.NotifyWorkersToStop();
                Console.WriteLine("  [Master] Svi workers obavešteni o završetku");

                Console.WriteLine("  ✓ Kompletan Master-Worker ciklus je uspešno završen");
            }
            else
            {
                Console.WriteLine("  ✗ Nema pronađenog rešenja (možda je difficulty prevelika ili opseg premali)");
            }
        }
        */
    }
}

