using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Klasa za parsiranje command-line argumenata.
    /// Subtask 4.2.3: CLI parametar za override broja niti
    /// </summary>
    public class CommandLineArgs
    {
        public int? ThreadCount { get; set; }
        public bool ShowHelp { get; set; }
        public uint? Difficulty { get; set; }
        public int? BlocksToMine { get; set; }

        /// <summary>
        /// Parsira command-line argumente
        /// </summary>
        /// <param name="args">Command-line argumenti</param>
        /// <returns>Parsirani argumenti</returns>
        public static CommandLineArgs Parse(string[] args)
        {
            var result = new CommandLineArgs();

            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i].ToLower();

                switch (arg)
                {
                    case "--threads":
                    case "-t":
                        if (i + 1 < args.Length)
                        {
                            if (int.TryParse(args[i + 1], out int threadCount) && threadCount > 0)
                            {
                                result.ThreadCount = threadCount;
                                i++; // Preskoči vrednost
                            }
                            else
                            {
                                Console.WriteLine($"Greška: '{args[i + 1]}' nije validan broj niti. Koristi pozitivan ceo broj.");
                            }
                        }
                        else
                        {
                            Console.WriteLine($"Greška: '{arg}' zahteva vrednost (broj niti).");
                        }
                        break;

                    case "--difficulty":
                    case "-d":
                        if (i + 1 < args.Length)
                        {
                            if (uint.TryParse(args[i + 1], out uint difficulty) && difficulty > 0)
                            {
                                result.Difficulty = difficulty;
                                i++; // Preskoči vrednost
                            }
                            else
                            {
                                Console.WriteLine($"Greška: '{args[i + 1]}' nije validna težina. Koristi pozitivan ceo broj.");
                            }
                        }
                        else
                        {
                            Console.WriteLine($"Greška: '{arg}' zahteva vrednost (težina).");
                        }
                        break;

                    case "--blocks":
                    case "-b":
                        if (i + 1 < args.Length)
                        {
                            if (int.TryParse(args[i + 1], out int blocks) && blocks > 0)
                            {
                                result.BlocksToMine = blocks;
                                i++; // Preskoči vrednost
                            }
                            else
                            {
                                Console.WriteLine($"Greška: '{args[i + 1]}' nije validan broj blokova. Koristi pozitivan ceo broj.");
                            }
                        }
                        else
                        {
                            Console.WriteLine($"Greška: '{arg}' zahteva vrednost (broj blokova).");
                        }
                        break;

                    case "--help":
                    case "-h":
                    case "/?":
                        result.ShowHelp = true;
                        break;

                    default:
                        Console.WriteLine($"Nepoznat argument: '{args[i]}'. Koristi --help za pomoć.");
                        break;
                }
            }

            return result;
        }

        /// <summary>
        /// Prikazuje help poruku sa svim dostupnim opcijama
        /// </summary>
        public static void PrintHelp()
        {
            Console.WriteLine("ParkingMate Blockchain - Paralelno rudarjenje blokova");
            Console.WriteLine();
            Console.WriteLine("Upotreba:");
            Console.WriteLine("  dotnet run [opcije]");
            Console.WriteLine();
            Console.WriteLine("Opcije:");
            Console.WriteLine("  -t, --threads <broj>    Broj niti za rudarjenje (override automatske detekcije)");
            Console.WriteLine("                         Ako nije navedeno, koristi se optimalan broj na osnovu CPU jezgara");
            Console.WriteLine("                         Primer: -t 8 ili --threads 16");
            Console.WriteLine();
            Console.WriteLine("  -d, --difficulty <broj> Težina rudarjenja (broj nula na početku hash-a)");
            Console.WriteLine("                         Default: 3");
            Console.WriteLine("                         Primer: -d 4 ili --difficulty 5");
            Console.WriteLine();
            Console.WriteLine("  -b, --blocks <broj>     Broj blokova za rudarenje");
            Console.WriteLine("                         Default: 30");
            Console.WriteLine("                         Primer: -b 10 ili --blocks 50");
            Console.WriteLine();
            Console.WriteLine("  -h, --help              Prikaži ovu help poruku");
            Console.WriteLine();
            Console.WriteLine("Primeri:");
            Console.WriteLine("  dotnet run                          # Automatska detekcija optimalnog broja niti");
            Console.WriteLine("  dotnet run --threads 8              # Koristi 8 niti");
            Console.WriteLine("  dotnet run -t 4 -d 4                # 4 niti, difficulty 4");
            Console.WriteLine("  dotnet run --threads 16 --blocks 10 # 16 niti, rudari 10 blokova");
            Console.WriteLine();
            Console.WriteLine("Informacije o CPU:");
            int logicalCores = ThreadedMiner.GetAvailableProcessorCount();
            int physicalCores = ThreadedMiner.GetPhysicalProcessorCount();
            int optimalThreads = ThreadedMiner.GetOptimalThreadCount();
            Console.WriteLine($"  Logički procesori: {logicalCores}");
            Console.WriteLine($"  Fizički jezgra (aproksimacija): {physicalCores}");
            Console.WriteLine($"  Preporučen broj niti (sa rezervisanim jezgrom): {optimalThreads}");
        }

        /// <summary>
        /// Dobija broj niti za rudarjenje - koristi override ako je postavljen, inače automatsku detekciju
        /// </summary>
        /// <returns>Broj niti za rudarjenje</returns>
        public int GetThreadCount()
        {
            if (ThreadCount.HasValue)
            {
                return ThreadCount.Value;
            }
            else
            {
                // Automatska detekcija optimalnog broja niti
                return ThreadedMiner.GetOptimalThreadCount();
            }
        }

        /// <summary>
        /// Dobija težinu za rudarjenje
        /// </summary>
        /// <returns>Težina za rudarjenje</returns>
        public uint GetDifficulty()
        {
            return Difficulty ?? 3; // Default: 3
        }

        /// <summary>
        /// Dobija broj blokova za rudarenje
        /// </summary>
        /// <returns>Broj blokova za rudarenje</returns>
        public int GetBlocksToMine()
        {
            return BlocksToMine ?? 30; // Default: 30
        }
    }
}

