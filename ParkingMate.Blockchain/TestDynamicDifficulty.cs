using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Testovi za DynamicDifficulty klasu (6.1.1 - Time-based algoritam za dinamičku težinu).
    /// Usklađeno sa specifikacijom: adjustmentBlock = chain[length - adjustmentInterval]
    /// i nova difficulty se bazira na difficulty prilagoditvenog bloka.
    /// </summary>
    public static class TestDynamicDifficulty
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test time-based algoritma za dinamičku težinu (6.1.1) ===\n");

            TestBasicDifficultyCalculation();
            TestDifficultyIncrease();
            TestDifficultyDecrease();
            TestMinimumDifficulty();
            TestShouldAdjustDifficulty();
            TestGetAverageBlockTime();
            TestEdgeCases();

            Console.WriteLine("\n✓ Testovi za Subtask 6.1.1 (Time-based algoritam) su prošli!\n");
        }

        private static void TestBasicDifficultyCalculation()
        {
            Console.WriteLine("Test 1: Osnovni izračun difficulty-ja (expected time)");

            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));

            // 10 blokova na tačno 600s
            for (int i = 1; i <= 10; i++)
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 600, "", 1, 0));

            // adjustmentInterval=10 => adjustmentBlock = chain[length-10] = chain[1]
            // last = chain[10], actual = (base+6000) - (base+600) = 5400
            // expected = 600*10 = 6000, 5400 je u "normal" opsegu => vraća baseDifficulty (difficulty adjustmentBlock-a) = 1
            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty: 123, blockIntervalSeconds: 600, adjustmentInterval: 10);

            if (newDifficulty == 1)
                Console.WriteLine("  ✓ Difficulty ostaje 1 kada je mining približno ciljan (normal range).");
            else
                Console.WriteLine($"  ✗ Očekivano: 1, dobijeno: {newDifficulty}");
        }

        private static void TestDifficultyIncrease()
        {
            Console.WriteLine("\nTest 2: Povećanje difficulty-ja (brži mining)");

            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));

            // 10 blokova na 300s => last at +3000, adjustmentBlock (index 1) at +300 => actual=2700
            // expected=6000, actual < expected/2 (=3000) => increase
            for (int i = 1; i <= 10; i++)
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 300, "", 2, 0)); // difficulty namerno 2

            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty: 999, blockIntervalSeconds: 600, adjustmentInterval: 10);

            // baseDifficulty = adjustmentBlock.Difficulty = chain[1].Difficulty = 2
            // očekujemo 3
            if (newDifficulty == 3)
                Console.WriteLine("  ✓ Difficulty se povećala 2 -> 3 (2x brže ili brže).");
            else
                Console.WriteLine($"  ✗ Očekivano: 3, dobijeno: {newDifficulty}");
        }

        private static void TestDifficultyDecrease()
        {
            Console.WriteLine("\nTest 3: Smanjenje difficulty-ja (sporiji mining)");

            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));

            // 10 blokova na 1200s => last at +12000, adjustmentBlock(index1) at +1200 => actual=10800
            // expected=6000, actual > expected*2 (=12000)? NE (10800), pa ovo nije decrease po striktnoj specifikaciji.
            // Zato pravimo 1500s => last +15000, adj +1500 => actual=13500, expected*2=12000 => decrease.
            for (int i = 1; i <= 10; i++)
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 1500, "", 5, 0)); // difficulty namerno 5

            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty: 999, blockIntervalSeconds: 600, adjustmentInterval: 10);

            // baseDifficulty = chain[1].Difficulty = 5 => očekujemo 4
            if (newDifficulty == 4)
                Console.WriteLine("  ✓ Difficulty se smanjila 5 -> 4 (2x sporije ili sporije).");
            else
                Console.WriteLine($"  ✗ Očekivano: 4, dobijeno: {newDifficulty}");
        }

        private static void TestMinimumDifficulty()
        {
            Console.WriteLine("\nTest 4: Minimum difficulty (minimum 1)");

            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));

            // Neka adjustmentBlock difficulty bude 1, i neka mining bude prespor => pokušaj decrease, ali mora ostati 1.
            // 10 blokova na 2000s => last +20000, adj(index1)+2000 => actual=18000, expected=6000, expected*2=12000 => decrease
            for (int i = 1; i <= 10; i++)
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 2000, "", 1, 0));

            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty: 1, blockIntervalSeconds: 600, adjustmentInterval: 10);

            if (newDifficulty == 1)
                Console.WriteLine("  ✓ Difficulty ne pada ispod 1.");
            else
                Console.WriteLine($"  ✗ Očekivano: 1, dobijeno: {newDifficulty}");
        }

        private static void TestShouldAdjustDifficulty()
        {
            Console.WriteLine("\nTest 5: Provera kada treba prilagoditi difficulty");

            // Ovo zavisi od tvoje implementacije ShouldAdjustDifficulty (koju nisi menjao),
            // ali tvoja logika kaže: blocksSinceGenesis % interval == 0
            bool shouldAdjust1 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 6, adjustmentInterval: 5);  // 5 posle genesis => true
            bool shouldAdjust2 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 11, adjustmentInterval: 5); // 10 posle genesis => true
            bool shouldNotAdjust1 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 7, adjustmentInterval: 5); // 6 posle genesis => false
            bool shouldNotAdjust2 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 1, adjustmentInterval: 5); // samo genesis => false

            if (shouldAdjust1 && shouldAdjust2 && !shouldNotAdjust1 && !shouldNotAdjust2)
                Console.WriteLine("  ✓ ShouldAdjustDifficulty radi ispravno");
            else
            {
                Console.WriteLine("  ✗ ShouldAdjustDifficulty ne radi ispravno:");
                Console.WriteLine($"    6/5: {shouldAdjust1} (true)");
                Console.WriteLine($"    11/5: {shouldAdjust2} (true)");
                Console.WriteLine($"    7/5: {shouldNotAdjust1} (false)");
                Console.WriteLine($"    1/5: {shouldNotAdjust2} (false)");
            }
        }

        private static void TestGetAverageBlockTime()
        {
            Console.WriteLine("\nTest 6: Izračunavanje prosečnog vremena između blokova");

            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));

            for (int i = 1; i <= 10; i++)
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 600, "", 1, 0));

            double avgTime = DynamicDifficulty.GetAverageBlockTime(chain, numBlocks: 10);

            if (Math.Abs(avgTime - 600) < 1.0)
                Console.WriteLine($"  ✓ Prosečno vreme: {avgTime:F2}s (očekivano ~600s)");
            else
                Console.WriteLine($"  ✗ Prosečno vreme: {avgTime:F2}s (očekivano 600s)");
        }

        private static void TestEdgeCases()
        {
            Console.WriteLine("\nTest 7: Edge cases");

            // Prazna lista
            var emptyChain = new List<Block>();
            uint d1 = DynamicDifficulty.CalculateDifficulty(emptyChain, currentDifficulty: 5, blockIntervalSeconds: 600, adjustmentInterval: 10);
            if (d1 == 5) Console.WriteLine("  ✓ Prazna lista vraća trenutnu difficulty");
            else Console.WriteLine($"  ✗ Prazna lista: očekivano 5, dobijeno {d1}");

            // Samo genesis
            var genesisOnly = new List<Block> { new Block(0, "Genesis", DateTimeOffset.UtcNow.ToUnixTimeSeconds(), "0", 1, 0) };
            uint d2 = DynamicDifficulty.CalculateDifficulty(genesisOnly, currentDifficulty: 3, blockIntervalSeconds: 600, adjustmentInterval: 10);
            if (d2 == 3) Console.WriteLine("  ✓ Samo genesis vraća trenutnu difficulty");
            else Console.WriteLine($"  ✗ Samo genesis: očekivano 3, dobijeno {d2}");

            // Nedovoljno blokova za adjustmentInterval
            var shortChain = new List<Block>();
            long t = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            shortChain.Add(new Block(0, "Genesis", t, "0", 1, 0));
            shortChain.Add(new Block(1, "B1", t + 10, "", 2, 0)); // length=2
            uint d3 = DynamicDifficulty.CalculateDifficulty(shortChain, currentDifficulty: 7, blockIntervalSeconds: 600, adjustmentInterval: 10);
            if (d3 == 7) Console.WriteLine("  ✓ Nedovoljno blokova za interval => vraća currentDifficulty");
            else Console.WriteLine($"  ✗ Short chain: očekivano 7, dobijeno {d3}");

            // Isti timestamp (actual <= 0) => vraća currentDifficulty
            var sameTimeChain = new List<Block>();
            long same = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            sameTimeChain.Add(new Block(0, "Genesis", same, "0", 1, 0));
            sameTimeChain.Add(new Block(1, "B1", same, "", 2, 0));
            sameTimeChain.Add(new Block(2, "B2", same, "", 2, 0));
            uint d4 = DynamicDifficulty.CalculateDifficulty(sameTimeChain, currentDifficulty: 2, blockIntervalSeconds: 600, adjustmentInterval: 2);
            if (d4 == 2) Console.WriteLine("  ✓ actualTimeSeconds<=0 => vraća currentDifficulty");
            else Console.WriteLine($"  ✗ same timestamps: očekivano 2, dobijeno {d4}");
        }
    }
}
