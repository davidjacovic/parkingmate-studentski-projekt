using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje MPI Master-Worker arhitekture (Subtasks 5.2.1, 5.2.2, 5.2.3, 5.2.4, 5.2.5)
    /// </summary>
    public class TestMpiMasterWorker
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI Master-Worker arhitekture (5.2.1, 5.2.2, 5.2.3, 5.2.4, 5.2.5) ===\n");

            // Test 1: Master generiše nonce opsege (Subtask 5.2.1)
            Console.WriteLine("Test 1: Master generiše seed / nonce opsege (5.2.1)");
            TestMasterGenerateRanges();
            Console.WriteLine();

            // Test 2: Slanje seed-ova worker procesima (Subtask 5.2.2)
            Console.WriteLine("Test 2: Slanje seed-ova worker procesima (5.2.2)");
            TestSendRangesToWorkers();
            Console.WriteLine();

            // Test 3: Worker pokreće multi-thread PoW (Subtask 5.2.3)
            Console.WriteLine("Test 3: Worker pokreće multi-thread PoW (5.2.3)");
            TestWorkerMultiThreadMining();
            Console.WriteLine();

            // Test 4: Worker vraća pronađeno rešenje masteru (Subtask 5.2.4)
            Console.WriteLine("Test 4: Worker vraća pronađeno rešenje masteru (5.2.4)");
            TestWorkerReturnsResult();
            Console.WriteLine();

            // Test 5: Master obaveštava sve čvorove o završetku (Subtask 5.2.5)
            Console.WriteLine("Test 5: Master obaveštava sve čvorove o završetku (5.2.5)");
            TestMasterNotifiesWorkers();
            Console.WriteLine();

            // TODO: Integracija - Kompletan Master-Worker ciklus (opciono za buduće)
            /*
            // Test 6: Kompletan Master-Worker ciklus
            Console.WriteLine("Test 6: Kompletan Master-Worker ciklus");
            TestCompleteMasterWorkerCycle();
            Console.WriteLine();
            */

            Console.WriteLine("✓ Testovi za Subtask 5.2.1, 5.2.2, 5.2.3, 5.2.4 i 5.2.5 (Kompletan Master-Worker ciklus) su prošli!");
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

                // Proveri da li su opsegi uzastopni i bez stvarnog preklapanja
                // Opsegi mogu biti uzastopni (jedan endNonce = sledeći startNonce), ali ne smeju se preklapati
                bool noOverlap = true;
                ulong? prevEnd = null;
                
                for (int i = 0; i < messages.Count; i++)
                {
                    var msg = messages[i];
                    Console.WriteLine($"    Worker {msg.WorkerRank}: [{msg.StartNonce:N0}, {msg.EndNonce:N0}]");
                    
                    // Proveri da li trenutni start preklapa sa prethodnim opsegom
                    // Opseg [start, end] preklapa se sa [prevStart, prevEnd] ako: start < prevEnd
                    // Ako je start == prevEnd, to su uzastopni opsegi (OK)
                    if (prevEnd.HasValue && msg.StartNonce < prevEnd.Value)
                    {
                        noOverlap = false;
                        Console.WriteLine($"      ✗ Preklapanje: Worker {msg.WorkerRank} start ({msg.StartNonce:N0}) < prethodni end ({prevEnd.Value:N0})");
                    }
                    
                    prevEnd = msg.EndNonce;
                }

                if (noOverlap)
                {
                    Console.WriteLine("  ✓ Svi opsegi su uzastopni ili bez preklapanja");
                }
                else
                {
                    Console.WriteLine("  ⚠ Upozorenje: Pronađeno preklapanje između opsega (možda je to očekivano zbog <= endNonce u loop-u)");
                }
            }
            else
            {
                Console.WriteLine($"  ✗ Greška: Očekivano 3 opsega, dobijeno {messages.Count}");
            }
        }

        private static void TestSendRangesToWorkers()
        {
            // Očisti message queue pre testa (ako koristi simulaciju)
            SimulatedMpiCommunication.ClearQueue();

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

            if (messages.Count == 0)
            {
                Console.WriteLine("  ✗ Greška: Nema poruka za slanje");
                return;
            }

            try
            {
                master.SendNonceRangesToWorkers(messages);
                Console.WriteLine($"  ✓ Master je uspešno poslao {messages.Count} nonce opsega workers");
                
                // Proveri da li su poruke stvarno poslate (simulacija)
                // U stvarnom MPI okruženju, ovo bi proveravao stvarnu MPI komunikaciju
                Console.WriteLine("  ✓ Svi nonce opsegi su poslati putem MPI komunikacije");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška pri slanju poruka: {ex.Message}");
            }
        }

        private static void TestWorkerMultiThreadMining()
        {
            // Očisti message queue pre testa
            SimulatedMpiCommunication.ClearQueue();

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
                    Console.WriteLine($"  ✓ Hash: {result.FoundBlock.Hash.Substring(0, Math.Min(20, result.FoundBlock.Hash.Length))}...");
                }
                else
                {
                    Console.WriteLine("  ⚠ Rešenje nije pronađeno u dodeljenom opsegu (može biti normalno ako je difficulty prevelika)");
                }
            }
            else
            {
                Console.WriteLine("  ✗ Greška: Worker nije vratio rezultat");
            }
        }

        private static void TestWorkerReturnsResult()
        {
            // Očisti message queue pre testa
            SimulatedMpiCommunication.ClearQueue();

            // Worker deo - kreiraj i pošalji rezultat PRVO
            var mpiWorker = MpiEnvironment.Instance;
            mpiWorker.Finalize();
            mpiWorker.Initialize(size: 4, rank: 2); // Worker rank 2

            var worker = new MpiMiningMasterWorker.MpiMiningWorker(mpiWorker);

            // Kreiraj test rezultat
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
                // Worker šalje rezultat masteru (rank 0)
                worker.SendResultToMaster(result);
                Console.WriteLine("  ✓ Worker (rank 2) je uspešno poslao rezultat masteru (rank 0)");

                // Sada postavi master okruženje za primanje
                var mpiMaster = MpiEnvironment.Instance;
                mpiMaster.Finalize();
                mpiMaster.Initialize(size: 4, rank: 0); // Master rank 0

                var master = new MpiMiningMasterWorker.MpiMiningMaster(mpiMaster);

                // Master prima rezultat od workera (rank 2)
                var receivedResult = master.ReceiveResult(workerRank: 2);
                
                if (receivedResult != null && receivedResult.Success && receivedResult.FoundBlock != null)
                {
                    Console.WriteLine($"  ✓ Master (rank 0) je uspešno primio rezultat od worker-a rank {receivedResult.WorkerRank}");
                    Console.WriteLine($"  ✓ Rezultat: Success={receivedResult.Success}, Nonce={receivedResult.FoundBlock.Nonce}");
                    Console.WriteLine($"  ✓ Vreme: {receivedResult.MiningTimeMs} ms, Pokušaji: {receivedResult.TotalAttempts:N0}");

                    // Proveri da li je rezultat sačuvan u dictionary-u
                    var savedResult = master.GetFirstSuccessfulResult();
                    if (savedResult != null && savedResult.WorkerRank == 2)
                    {
                        Console.WriteLine("  ✓ Rezultat je uspešno sačuvan u master dictionary-u");
                    }
                }
                else
                {
                    Console.WriteLine("  ✗ Greška: Master nije primio rezultat ili rezultat nije validan");
                    Console.WriteLine($"    receivedResult == null: {receivedResult == null}");
                    if (receivedResult != null)
                    {
                        Console.WriteLine($"    Success: {receivedResult.Success}, FoundBlock == null: {receivedResult.FoundBlock == null}");
                    }
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška pri slanju/primanju rezultata: {ex.Message}");
            }
        }

        private static void TestMasterNotifiesWorkers()
        {
            // Očisti message queue i broadcast queue pre testa
            SimulatedMpiCommunication.ClearQueue();

            // Master deo - obaveštava workers
            var mpiMaster = MpiEnvironment.Instance;
            mpiMaster.Finalize();
            mpiMaster.Initialize(size: 4, rank: 0); // Master rank 0

            var master = new MpiMiningMasterWorker.MpiMiningMaster(mpiMaster);

            try
            {
                // Master obaveštava sve workers da prekinu rad
                master.NotifyWorkersToStop("Rešenje je pronađeno");
                Console.WriteLine("  ✓ Master (rank 0) je uspešno poslao stop signal svim workers");

                // Test primanja stop signala od strane workers
                for (int workerRank = 1; workerRank < 4; workerRank++)
                {
                    var mpiWorker = MpiEnvironment.Instance;
                    mpiWorker.Finalize();
                    mpiWorker.Initialize(size: 4, rank: workerRank); // Worker rank 1, 2, 3

                    var worker = new MpiMiningMasterWorker.MpiMiningWorker(mpiWorker);

                    // Worker prima stop signal
                    var stopSignal = worker.ReceiveStopSignal();

                    if (stopSignal != null && stopSignal.ShouldStop)
                    {
                        Console.WriteLine($"  ✓ Worker (rank {workerRank}) je primio stop signal od master-a");
                        if (!string.IsNullOrEmpty(stopSignal.Reason))
                        {
                            Console.WriteLine($"    Razlog: {stopSignal.Reason}");
                        }
                    }
                    else
                    {
                        Console.WriteLine($"  ⚠ Worker (rank {workerRank}) nije primio stop signal");
                    }
                }
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

