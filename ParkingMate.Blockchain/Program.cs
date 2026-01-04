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

            // Napomena: Sledeći kod testira CLI parametre i blockchain funkcionalnost

            // Dobij broj niti (CLI override ili automatska detekcija)
            int threadCount = cliArgs.GetThreadCount();
            uint difficulty = cliArgs.GetDifficulty();
            int blocksToMine = cliArgs.GetBlocksToMine();

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
            Console.WriteLine($"Težina: {difficulty}");
            Console.WriteLine($"Broj blokova za rudarenje: {blocksToMine}");
            Console.WriteLine();

            var blockchain = new Blockchain();

            // Napomena: Trenutna implementacija blockchain.AddBlock koristi single-threaded mining
            // U budućim verzijama, ovo može biti zamenjeno multi-threaded mining-om koristeći ThreadedMiner
            // sa threadCount niti
            Console.WriteLine($"Napomena: Trenutno se koristi single-threaded mining.");
            Console.WriteLine($"Konfigurisano {threadCount} niti će biti dostupno za multi-threaded mining u budućim verzijama.");
            Console.WriteLine();

            for (int i = 1; i <= blocksToMine; i++)
            {
                var block = new Block(
                    index: 0,
                    data: $"Auto-mined block #{i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "",
                    difficulty: difficulty,
                    nonce: 0
                );

                Console.WriteLine($"\nMining block {i}...");
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
