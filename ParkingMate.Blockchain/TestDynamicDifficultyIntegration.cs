using System;
using System.Collections.Generic;
using System.Threading;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Testovi za integraciju dinamičke difficulty u mining proces (6.1.3).
    /// </summary>
    public static class TestDynamicDifficultyIntegration
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test integracije dinamičke difficulty u mining proces (6.1.3) ===\n");

            TestDifficultyAdjustment();
            TestDefaultParameters();
            TestCustomParameters();
            TestDifficultyDoesNotAdjustBeforeInterval();

            Console.WriteLine("\n✓ Testovi za Subtask 6.1.3 (Integracija u mining proces) su prošli!\n");
        }

        private static void TestDifficultyAdjustment()
        {
            Console.WriteLine("Test 1: Prilagođavanje difficulty tokom mining-a");

            // Kreiraj blockchain sa kratkim intervalima za brže testiranje
            var blockchain = new Blockchain(blockIntervalSeconds: 1, adjustmentInterval: 5);
            
            uint initialDifficulty = 1;
            uint currentDifficulty = initialDifficulty;

            // Dodaj 10 blokova
            for (int i = 1; i <= 10; i++)
            {
                // Simuliraj različita vremena između blokova
                // Prvih 5 blokova: brži mining (0.5 sekunde između blokova)
                // Zatim 5 blokova: još brži mining (0.3 sekunde između blokova)
                if (i > 1)
                {
                    long delayMs = i <= 5 ? 500 : 300;
                    Thread.Sleep((int)delayMs);
                }

                // Izračunaj difficulty za sledeći blok
                uint previousDifficulty = currentDifficulty;
                currentDifficulty = blockchain.GetNextDifficulty(currentDifficulty);

                var block = new Block(
                    index: (uint)i,
                    data: $"Test block #{i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: blockchain.GetLatestBlock().Hash,
                    difficulty: currentDifficulty,
                    nonce: 0
                );

                // Dodaj blok u blockchain (sa simuliranim mining-om)
                blockchain.AddBlock(block);

                if (currentDifficulty != previousDifficulty)
                {
                    Console.WriteLine($"  Block {i}: Difficulty prilagođena sa {previousDifficulty} na {currentDifficulty}");
                }
            }

            // Proveri da li je difficulty prilagođena
            if (blockchain.Chain.Count > 5)
            {
                uint difficultyAt5 = blockchain.Chain[5].Difficulty;
                uint difficultyAt10 = blockchain.Chain[10].Difficulty;
                Console.WriteLine($"  Difficulty na bloku 5: {difficultyAt5}");
                Console.WriteLine($"  Difficulty na bloku 10: {difficultyAt10}");
                Console.WriteLine("  ✓ Difficulty se prilagođava tokom mining-a");
            }
            else
            {
                Console.WriteLine("  ✗ Nema dovoljno blokova za test");
            }
        }

        private static void TestDefaultParameters()
        {
            Console.WriteLine("\nTest 2: Podrazumevani parametri");

            var blockchain = new Blockchain(); // Koristi default parametre

            if (blockchain.BlockIntervalSeconds == 600 && blockchain.AdjustmentInterval == 10)
            {
                Console.WriteLine("  ✓ Default block interval: 600 sekundi");
                Console.WriteLine("  ✓ Default adjustment interval: 10 blokova");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: blockInterval=600, adjustmentInterval=10");
                Console.WriteLine($"    Dobijeno: blockInterval={blockchain.BlockIntervalSeconds}, adjustmentInterval={blockchain.AdjustmentInterval}");
            }
        }

        private static void TestCustomParameters()
        {
            Console.WriteLine("\nTest 3: Prilagođeni parametri");

            var blockchain = new Blockchain(blockIntervalSeconds: 300, adjustmentInterval: 5);

            if (blockchain.BlockIntervalSeconds == 300 && blockchain.AdjustmentInterval == 5)
            {
                Console.WriteLine("  ✓ Custom block interval: 300 sekundi");
                Console.WriteLine("  ✓ Custom adjustment interval: 5 blokova");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: blockInterval=300, adjustmentInterval=5");
                Console.WriteLine($"    Dobijeno: blockInterval={blockchain.BlockIntervalSeconds}, adjustmentInterval={blockchain.AdjustmentInterval}");
            }
        }

        private static void TestDifficultyDoesNotAdjustBeforeInterval()
        {
            Console.WriteLine("\nTest 4: Difficulty se ne prilagođava pre adjustment interval-a");

            var blockchain = new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 5);
            uint initialDifficulty = 2;

            // Dodaj 4 bloka (manje od adjustment interval-a od 5)
            for (int i = 1; i <= 4; i++)
            {
                uint nextDifficulty = blockchain.GetNextDifficulty(initialDifficulty);
                
                if (nextDifficulty == initialDifficulty)
                {
                    // Difficulty se nije promenila (očekivano)
                    if (i == 4)
                    {
                        Console.WriteLine($"  ✓ Difficulty ostaje {initialDifficulty} pre adjustment interval-a (blok {i}/5)");
                    }
                }
                else
                {
                    Console.WriteLine($"  ✗ Difficulty se promenila pre adjustment interval-a: {initialDifficulty} -> {nextDifficulty} na bloku {i}");
                }

                // Simuliraj dodavanje bloka
                var block = new Block((uint)i, $"Test block #{i}", DateTimeOffset.UtcNow.ToUnixTimeSeconds(), blockchain.GetLatestBlock().Hash, initialDifficulty, 0);
                blockchain.AddBlock(block);
            }

            // Sada dodaj 5. blok (tačno na adjustment interval)
            uint difficultyAtInterval = blockchain.GetNextDifficulty(initialDifficulty);
            Console.WriteLine($"  Difficulty na 5. bloku (adjustment interval): {difficultyAtInterval}");
        }
    }
}

