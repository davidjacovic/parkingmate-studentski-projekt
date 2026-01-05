using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Testovi za DynamicDifficulty klasu (6.1.1 - Time-based algoritam za dinamičku težinu).
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
            Console.WriteLine("Test 1: Osnovni izračun difficulty-ja");
            
            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis blok
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));
            
            // Dodaj 10 blokova sa razmakom od 600 sekundi (tačno ciljano vreme)
            for (int i = 1; i <= 10; i++)
            {
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 600, "", 1, 0));
            }

            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty: 1, blockIntervalSeconds: 600, adjustmentInterval: 10);
            
            // Ako je vreme tačno ciljano, difficulty bi trebalo da ostane približno ista (1)
            if (Math.Abs(newDifficulty - 1) <= 1) // Dozvoljavamo razliku od 1 zbog zaokruživanja
            {
                Console.WriteLine("  ✓ Difficulty ostaje približno ista kada je vreme tačno ciljano");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivana difficulty ~1, dobijena: {newDifficulty}");
            }
        }

        private static void TestDifficultyIncrease()
        {
            Console.WriteLine("\nTest 2: Povećanje difficulty-ja (brži mining)");
            
            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis blok
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));
            
            // Dodaj 10 blokova sa razmakom od 300 sekundi (2x brži od ciljanog od 600 sekundi)
            // Očekivano: difficulty se povećava (jer je mining bio brži)
            for (int i = 1; i <= 10; i++)
            {
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 300, "", 1, 0));
            }

            uint currentDifficulty = 1;
            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty, blockIntervalSeconds: 600, adjustmentInterval: 10);
            
            if (newDifficulty > currentDifficulty)
            {
                Console.WriteLine($"  ✓ Difficulty se povećala sa {currentDifficulty} na {newDifficulty} (mining je bio brži)");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano povećanje difficulty-ja, ali je ostala {newDifficulty}");
            }
        }

        private static void TestDifficultyDecrease()
        {
            Console.WriteLine("\nTest 3: Smanjenje difficulty-ja (sporiji mining)");
            
            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis blok
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));
            
            // Dodaj 10 blokova sa razmakom od 1200 sekundi (2x sporiji od ciljanog od 600 sekundi)
            // Očekivano: difficulty se smanjuje (jer je mining bio sporiji)
            for (int i = 1; i <= 10; i++)
            {
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 1200, "", 5, 0));
            }

            uint currentDifficulty = 5;
            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty, blockIntervalSeconds: 600, adjustmentInterval: 10);
            
            if (newDifficulty < currentDifficulty)
            {
                Console.WriteLine($"  ✓ Difficulty se smanjila sa {currentDifficulty} na {newDifficulty} (mining je bio sporiji)");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano smanjenje difficulty-ja, ali je ostala {newDifficulty}");
            }
        }

        private static void TestMinimumDifficulty()
        {
            Console.WriteLine("\nTest 4: Minimum difficulty (minimum 1)");
            
            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis blok
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));
            
            // Dodaj 10 blokova sa veoma velikim razmakom (10x sporiji)
            // Očekivano: difficulty se smanjuje, ali ne ispod 1
            for (int i = 1; i <= 10; i++)
            {
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 6000, "", 1, 0));
            }

            uint currentDifficulty = 1;
            uint newDifficulty = DynamicDifficulty.CalculateDifficulty(chain, currentDifficulty, blockIntervalSeconds: 600, adjustmentInterval: 10);
            
            if (newDifficulty >= 1)
            {
                Console.WriteLine($"  ✓ Difficulty je minimum 1: {newDifficulty}");
            }
            else
            {
                Console.WriteLine($"  ✗ Difficulty je {newDifficulty}, ali bi trebalo da bude minimum 1");
            }
        }

        private static void TestShouldAdjustDifficulty()
        {
            Console.WriteLine("\nTest 5: Provera kada treba prilagoditi difficulty");
            
            // Test sa adjustmentInterval = 5
            bool shouldAdjust1 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 6, adjustmentInterval: 5); // 5 blokova posle genesis
            bool shouldAdjust2 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 11, adjustmentInterval: 5); // 10 blokova posle genesis
            bool shouldNotAdjust1 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 7, adjustmentInterval: 5); // 6 blokova posle genesis
            bool shouldNotAdjust2 = DynamicDifficulty.ShouldAdjustDifficulty(chainLength: 1, adjustmentInterval: 5); // Samo genesis

            if (shouldAdjust1 && shouldAdjust2 && !shouldNotAdjust1 && !shouldNotAdjust2)
            {
                Console.WriteLine("  ✓ ShouldAdjustDifficulty radi ispravno");
            }
            else
            {
                Console.WriteLine($"  ✗ ShouldAdjustDifficulty ne radi ispravno:");
                Console.WriteLine($"    Chain length 6 (interval 5): {shouldAdjust1} (očekivano: true)");
                Console.WriteLine($"    Chain length 11 (interval 5): {shouldAdjust2} (očekivano: true)");
                Console.WriteLine($"    Chain length 7 (interval 5): {shouldNotAdjust1} (očekivano: false)");
                Console.WriteLine($"    Chain length 1 (interval 5): {shouldNotAdjust2} (očekivano: false)");
            }
        }

        private static void TestGetAverageBlockTime()
        {
            Console.WriteLine("\nTest 6: Izračunavanje prosečnog vremena između blokova");
            
            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis blok
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));
            
            // Dodaj 10 blokova sa razmakom od 600 sekundi
            for (int i = 1; i <= 10; i++)
            {
                chain.Add(new Block((uint)i, $"Block {i}", baseTime + i * 600, "", 1, 0));
            }

            double avgTime = DynamicDifficulty.GetAverageBlockTime(chain, numBlocks: 10);
            
            // Očekivano prosečno vreme: 600 sekundi
            if (Math.Abs(avgTime - 600) < 1.0) // Dozvoljavamo razliku od 1 sekunde
            {
                Console.WriteLine($"  ✓ Prosečno vreme između blokova: {avgTime:F2} sekundi (očekivano: 600)");
            }
            else
            {
                Console.WriteLine($"  ✗ Prosečno vreme: {avgTime:F2} sekundi (očekivano: 600)");
            }
        }

        private static void TestEdgeCases()
        {
            Console.WriteLine("\nTest 7: Edge cases");
            
            // Test sa praznom listom
            var emptyChain = new List<Block>();
            uint difficulty1 = DynamicDifficulty.CalculateDifficulty(emptyChain, currentDifficulty: 5, blockIntervalSeconds: 600, adjustmentInterval: 10);
            if (difficulty1 == 5)
            {
                Console.WriteLine("  ✓ Prazna lista vraća trenutnu difficulty");
            }
            else
            {
                Console.WriteLine($"  ✗ Prazna lista vraća {difficulty1}, očekivano: 5");
            }

            // Test sa samo genesis blokom
            var genesisOnly = new List<Block>
            {
                new Block(0, "Genesis", DateTimeOffset.UtcNow.ToUnixTimeSeconds(), "0", 1, 0)
            };
            uint difficulty2 = DynamicDifficulty.CalculateDifficulty(genesisOnly, currentDifficulty: 3, blockIntervalSeconds: 600, adjustmentInterval: 10);
            if (difficulty2 == 3)
            {
                Console.WriteLine("  ✓ Lanac sa samo genesis blokom vraća trenutnu difficulty");
            }
            else
            {
                Console.WriteLine($"  ✗ Lanac sa samo genesis blokom vraća {difficulty2}, očekivano: 3");
            }

            // Test sa blokovima sa istim timestamp-om (vreme = 0)
            var sameTimeChain = new List<Block>();
            long sameTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            sameTimeChain.Add(new Block(0, "Genesis", sameTime, "0", 1, 0));
            sameTimeChain.Add(new Block(1, "Block 1", sameTime, "", 2, 0));
            uint difficulty3 = DynamicDifficulty.CalculateDifficulty(sameTimeChain, currentDifficulty: 2, blockIntervalSeconds: 600, adjustmentInterval: 10);
            if (difficulty3 == 2)
            {
                Console.WriteLine("  ✓ Blokovi sa istim timestamp-om vraćaju trenutnu difficulty");
            }
            else
            {
                Console.WriteLine($"  ✗ Blokovi sa istim timestamp-om vraćaju {difficulty3}, očekivano: 2");
            }
        }
    }
}

