using System;

namespace ParkingMate.Blockchain
{
    class Program
    {
        static void Main(string[] args)
        {
            // Parsiranje command-line argumenata
            var cliArgs = CommandLineArgs.Parse(args);

            // Prikaži help ako je tražen
            if (cliArgs.ShowHelp)
            {
                CommandLineArgs.PrintHelp();
                return;
            }

            // Test ThreadPool implementacije (4.1.2, 4.1.3, 4.1.4)
            // Otkomentariši sledeću liniju da testiraš ThreadPool:
            // TestThreadPool.RunTest();
            // return;

            // Test skaliranja (4.1.5)
            // Otkomentariši sledeću liniju da testiraš skaliranje sa različitim brojevima niti:
            // TestScaling.RunTest();
            // return;

            // Test detekcije CPU jezgara (4.2.1)
            // Otkomentariši sledeću liniju da testiraš detekciju CPU jezgara:
            // TestCpuDetection.RunTest();
            // return;

            // Test MPI inicijalizacije (5.1.1, 5.1.2, 5.1.3)
            // Otkomentariši sledeću liniju da testiraš MPI inicijalizaciju:
            // TestMpiInitialization.RunTest();
            // return;

            // Test MPI Master-Worker arhitekture (5.2.1-5.2.5)
            // Otkomentariši sledeću liniju da testiraš Master-Worker arhitekturu:
            // TestMpiMasterWorker.RunTest();
            // return;

            // Test MPI sinhronizacije i prekida (5.3.1, 5.3.2, 5.3.3)
            // Otkomentariši sledeću liniju da testiraš MPI sinhronizaciju:
            // TestMpiSynchronization.RunTest();
            // return;

            // Test dinamičke težine (6.1.1)
            // Otkomentariši sledeću liniju da testiraš time-based algoritam:
            // TestDynamicDifficulty.RunTest();
            // return;

            // Test parametara dinamičke težine (6.1.2)
            // Otkomentariši sledeću liniju da testiraš CLI parametre:
            // TestDynamicDifficultyParams.RunTest();
            // return;

            // Test integracije dinamičke težine u mining proces (6.1.3)
            // Otkomentariši sledeću liniju da testiraš integraciju:
            // TestDynamicDifficultyIntegration.RunTest();
            // return;

            // Test izračunavanja kumulativne težine (6.2.1)
            // Otkomentariši sledeću liniju da testiraš izračunavanje 2^difficulty:
            TestCumulativeWeight.RunTest();
            return;

            // MPI inicijalizacija (5.1.1, 5.1.2, 5.1.3)
            var mpi = MpiEnvironment.Instance;
            if (cliArgs.UseMpi)
            {
                // Inicijalizuj MPI okruženje
                int mpiSize = cliArgs.MpiSize ?? 1;
                int mpiRank = cliArgs.MpiRank ?? 0;
                
                if (!mpi.Initialize(mpiSize, mpiRank))
                {
                    Console.WriteLine("Upozorenje: MPI okruženje je već inicijalizovano. Nastavljam sa postojećom konfiguracijom.");
                }

                Console.WriteLine("=== MPI okruženje ===");
                Console.WriteLine(mpi);
                Console.WriteLine($"IsMaster: {mpi.IsMaster}, IsWorker: {mpi.IsWorker}");
                Console.WriteLine();
            }
            else
            {
                // Pokušaj inicijalizaciju iz environment varijabli ili argumenata (ako su postavljeni)
                // Ovo omogućava automatsku detekciju u stvarnom MPI okruženju
                mpi.InitializeFromArgs(args);
                if (mpi.IsInitialized)
                {
                    Console.WriteLine("=== MPI okruženje (detektovano automatski) ===");
                    Console.WriteLine(mpi);
                    Console.WriteLine();
                }
            }

            // Napomena: Sledeći kod testira CLI parametre i blockchain funkcionalnost

            // Dobij broj niti (CLI override ili automatska detekcija)
            int threadCount = cliArgs.GetThreadCount();
            uint initialDifficulty = cliArgs.GetDifficulty();
            int blocksToMine = cliArgs.GetBlocksToMine();
            
            // Dobij parametre za dinamičku difficulty (6.1.3)
            long blockIntervalSeconds = cliArgs.GetBlockIntervalSeconds();
            uint adjustmentInterval = cliArgs.GetAdjustmentInterval();

            // Prikaži informacije o konfiguraciji
            Console.WriteLine("=== Konfiguracija rudarjenja ===");
            if (cliArgs.ThreadCount.HasValue)
            {
                Console.WriteLine($"Broj niti (CLI override): {threadCount}");
            }
            else
            {
                int autoThreads = ThreadedMiner.GetOptimalThreadCount();
                Console.WriteLine($"Broj niti (automatska detekcija): {threadCount}");
                Console.WriteLine($"  Dostupno logičkih procesora: {ThreadedMiner.GetAvailableProcessorCount()}");
                Console.WriteLine($"  Dostupno fizičkih jezgara: {ThreadedMiner.GetPhysicalProcessorCount()}");
            }
            Console.WriteLine($"Početna težina: {initialDifficulty}");
            Console.WriteLine($"Broj blokova za rudarenje: {blocksToMine}");
            Console.WriteLine($"Block interval: {blockIntervalSeconds} sekundi (6.1.3)");
            Console.WriteLine($"Adjustment interval: {adjustmentInterval} blokova (6.1.3)");
            Console.WriteLine();

            // Kreiraj blockchain sa parametrima za dinamičku difficulty (6.1.3)
            var blockchain = new Blockchain(blockIntervalSeconds, adjustmentInterval);

            // Napomena: Trenutna implementacija blockchain.AddBlock koristi single-threaded mining
            // U budućim verzijama, ovo može biti zamenjeno multi-threaded mining-om koristeći ThreadedMiner
            // sa threadCount niti
            Console.WriteLine($"Napomena: Trenutno se koristi single-threaded mining.");
            Console.WriteLine($"Konfigurisano {threadCount} niti će biti dostupno za multi-threaded mining u budućim verzijama.");
            Console.WriteLine();

            // Trenutna difficulty vrednost (počinje sa početnom difficulty, zatim se dinamički prilagođava)
            uint currentDifficulty = initialDifficulty;

            for (int i = 1; i <= blocksToMine; i++)
            {
                // Izračunaj difficulty za sledeći blok (6.1.3)
                currentDifficulty = blockchain.GetNextDifficulty(currentDifficulty);
                
                if (i > 1 && DynamicDifficulty.ShouldAdjustDifficulty(blockchain.Chain.Count, adjustmentInterval))
                {
                    Console.WriteLine($"  [Difficulty prilagođena na: {currentDifficulty}]");
                }

                var block = new Block(
                    index: 0,
                    data: $"Auto-mined block #{i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "",
                    difficulty: currentDifficulty,
                    nonce: 0
                );

                Console.WriteLine($"\nMining block {i} (difficulty: {currentDifficulty})...");
                blockchain.AddBlock(block);
            }

            Console.WriteLine("\n=== Final Blockchain ===");
            foreach (var block in blockchain.Chain)
            {
                Console.WriteLine(block);
            }

            Console.WriteLine("Blockchain valid: " + blockchain.IsValidChain());
        }
    }
}
