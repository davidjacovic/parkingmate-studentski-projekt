using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Testovi za CLI parametre dinamičke težine (6.1.2 - Parametri: block interval, adjustment interval).
    /// </summary>
    public static class TestDynamicDifficultyParams
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test CLI parametara za dinamičku težinu (6.1.2) ===\n");

            TestBlockIntervalParsing();
            TestAdjustmentIntervalParsing();
            TestDefaultValues();
            TestInvalidValues();
            TestCombinedParameters();
            TestParametersWithDynamicDifficulty();

            Console.WriteLine("\n✓ Testovi za Subtask 6.1.2 (Parametri: block interval, adjustment interval) su prošli!\n");
        }

        private static void TestBlockIntervalParsing()
        {
            Console.WriteLine("Test 1: Parsiranje block interval parametra");

            // Test --block-interval
            var args1 = new[] { "--block-interval", "300" };
            var cliArgs1 = CommandLineArgs.Parse(args1);
            if (cliArgs1.BlockIntervalSeconds == 300)
            {
                Console.WriteLine("  ✓ --block-interval 300 je uspešno parsiran");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: 300, dobijeno: {cliArgs1.BlockIntervalSeconds}");
            }

            // Test -bi
            var args2 = new[] { "-bi", "1200" };
            var cliArgs2 = CommandLineArgs.Parse(args2);
            if (cliArgs2.BlockIntervalSeconds == 1200)
            {
                Console.WriteLine("  ✓ -bi 1200 je uspešno parsiran");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: 1200, dobijeno: {cliArgs2.BlockIntervalSeconds}");
            }

            // Test GetBlockIntervalSeconds() metoda
            if (cliArgs1.GetBlockIntervalSeconds() == 300 && cliArgs2.GetBlockIntervalSeconds() == 1200)
            {
                Console.WriteLine("  ✓ GetBlockIntervalSeconds() metoda vraća ispravne vrednosti");
            }
            else
            {
                Console.WriteLine("  ✗ GetBlockIntervalSeconds() metoda ne vraća ispravne vrednosti");
            }
        }

        private static void TestAdjustmentIntervalParsing()
        {
            Console.WriteLine("\nTest 2: Parsiranje adjustment interval parametra");

            // Test --adjustment-interval
            var args1 = new[] { "--adjustment-interval", "5" };
            var cliArgs1 = CommandLineArgs.Parse(args1);
            if (cliArgs1.AdjustmentInterval == 5)
            {
                Console.WriteLine("  ✓ --adjustment-interval 5 je uspešno parsiran");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: 5, dobijeno: {cliArgs1.AdjustmentInterval}");
            }

            // Test -ai
            var args2 = new[] { "-ai", "20" };
            var cliArgs2 = CommandLineArgs.Parse(args2);
            if (cliArgs2.AdjustmentInterval == 20)
            {
                Console.WriteLine("  ✓ -ai 20 je uspešno parsiran");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: 20, dobijeno: {cliArgs2.AdjustmentInterval}");
            }

            // Test GetAdjustmentInterval() metoda
            if (cliArgs1.GetAdjustmentInterval() == 5 && cliArgs2.GetAdjustmentInterval() == 20)
            {
                Console.WriteLine("  ✓ GetAdjustmentInterval() metoda vraća ispravne vrednosti");
            }
            else
            {
                Console.WriteLine("  ✗ GetAdjustmentInterval() metoda ne vraća ispravne vrednosti");
            }
        }

        private static void TestDefaultValues()
        {
            Console.WriteLine("\nTest 3: Default vrednosti parametara");

            var args = Array.Empty<string>();
            var cliArgs = CommandLineArgs.Parse(args);

            // Proveri default vrednosti
            if (cliArgs.GetBlockIntervalSeconds() == 600)
            {
                Console.WriteLine("  ✓ Default block interval je 600 sekundi (10 minuta)");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano default: 600, dobijeno: {cliArgs.GetBlockIntervalSeconds()}");
            }

            if (cliArgs.GetAdjustmentInterval() == 10)
            {
                Console.WriteLine("  ✓ Default adjustment interval je 10 blokova");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano default: 10, dobijeno: {cliArgs.GetAdjustmentInterval()}");
            }
        }

        private static void TestInvalidValues()
        {
            Console.WriteLine("\nTest 4: Nevalidne vrednosti parametara");

            // Test negativne vrednosti za block interval
            var args1 = new[] { "--block-interval", "-100" };
            var cliArgs1 = CommandLineArgs.Parse(args1);
            if (!cliArgs1.BlockIntervalSeconds.HasValue)
            {
                Console.WriteLine("  ✓ Negativna vrednost za block interval je odbijena");
            }
            else
            {
                Console.WriteLine($"  ✗ Negativna vrednost je prihvaćena: {cliArgs1.BlockIntervalSeconds}");
            }

            // Test nula za block interval
            var args2 = new[] { "--block-interval", "0" };
            var cliArgs2 = CommandLineArgs.Parse(args2);
            if (!cliArgs2.BlockIntervalSeconds.HasValue)
            {
                Console.WriteLine("  ✓ Nula za block interval je odbijena");
            }
            else
            {
                Console.WriteLine($"  ✗ Nula je prihvaćena: {cliArgs2.BlockIntervalSeconds}");
            }

            // Test nevalidan string za adjustment interval
            var args3 = new[] { "--adjustment-interval", "abc" };
            var cliArgs3 = CommandLineArgs.Parse(args3);
            if (!cliArgs3.AdjustmentInterval.HasValue)
            {
                Console.WriteLine("  ✓ Nevalidan string za adjustment interval je odbijen");
            }
            else
            {
                Console.WriteLine($"  ✗ Nevalidan string je prihvaćen: {cliArgs3.AdjustmentInterval}");
            }

            // Test nula za adjustment interval
            var args4 = new[] { "--adjustment-interval", "0" };
            var cliArgs4 = CommandLineArgs.Parse(args4);
            if (!cliArgs4.AdjustmentInterval.HasValue)
            {
                Console.WriteLine("  ✓ Nula za adjustment interval je odbijena");
            }
            else
            {
                Console.WriteLine($"  ✗ Nula je prihvaćena: {cliArgs4.AdjustmentInterval}");
            }
        }

        private static void TestCombinedParameters()
        {
            Console.WriteLine("\nTest 5: Kombinovani parametri");

            var args = new[] { "--block-interval", "300", "--adjustment-interval", "5", "--threads", "4", "--difficulty", "3" };
            var cliArgs = CommandLineArgs.Parse(args);

            if (cliArgs.GetBlockIntervalSeconds() == 300 &&
                cliArgs.GetAdjustmentInterval() == 5 &&
                cliArgs.ThreadCount == 4 &&
                cliArgs.Difficulty == 3)
            {
                Console.WriteLine("  ✓ Kombinovani parametri se pravilno parsiraju");
            }
            else
            {
                Console.WriteLine($"  ✗ Kombinovani parametri nisu pravilno parsirani:");
                Console.WriteLine($"    Block interval: {cliArgs.GetBlockIntervalSeconds()} (očekivano: 300)");
                Console.WriteLine($"    Adjustment interval: {cliArgs.GetAdjustmentInterval()} (očekivano: 5)");
                Console.WriteLine($"    Threads: {cliArgs.ThreadCount} (očekivano: 4)");
                Console.WriteLine($"    Difficulty: {cliArgs.Difficulty} (očekivano: 3)");
            }
        }

        private static void TestParametersWithDynamicDifficulty()
        {
            Console.WriteLine("\nTest 6: Korišćenje parametara sa DynamicDifficulty algoritmom");

            // Test sa custom block interval i adjustment interval
            var chain = new System.Collections.Generic.List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis blok
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));

            // Dodaj 5 blokova sa razmakom od 150 sekundi (brži mining - custom block interval je 300)
            for (int i = 1; i <= 5; i++)
            {
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 150, "", 1, 0));
            }

            // Test sa custom parametrima (block interval = 300, adjustment interval = 5)
            uint currentDifficulty = 1;
            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(
                chain,
                currentDifficulty,
                blockIntervalSeconds: 300,  // Custom block interval
                adjustmentInterval: 5       // Custom adjustment interval
            );

            // Mining je bio brži (150s < 300s), pa bi difficulty trebalo da se poveća
            if (newDifficulty > currentDifficulty)
            {
                Console.WriteLine($"  ✓ Difficulty se prilagođava sa custom parametrima: {currentDifficulty} -> {newDifficulty}");
            }
            else
            {
                Console.WriteLine($"  ✗ Difficulty se nije prilagodila: {currentDifficulty} -> {newDifficulty}");
            }

            // Test sa default parametrima za poređenje
            uint newDifficultyDefault = DynamicDifficulty.CalculateDifficulty(
                chain,
                currentDifficulty,
                blockIntervalSeconds: 600,  // Default block interval
                adjustmentInterval: 10      // Default adjustment interval
            );

            Console.WriteLine($"  Custom parametri: block interval=300s, adjustment interval=5, nova difficulty={newDifficulty}");
            Console.WriteLine($"  Default parametri: block interval=600s, adjustment interval=10, nova difficulty={newDifficultyDefault}");
        }
    }
}

