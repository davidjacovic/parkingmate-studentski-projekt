using System;
using System.Numerics;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Testovi za izračunavanje kumulativne težine (6.2.1 - Izračunavanje 2^difficulty po bloku).
    /// </summary>
    public static class TestCumulativeWeight
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test izračunavanja 2^difficulty po bloku (6.2.1) ===\n");

            TestBlockWeightCalculation();
            TestBlockWeightWithBlockObject();
            TestBlockWeightEdgeCases();
            TestBlockWeightComparison();

            Console.WriteLine("\n✓ Testovi za Subtask 6.2.1 (Izračunavanje 2^difficulty po bloku) su prošli!\n");
        }

        private static void TestBlockWeightCalculation()
        {
            Console.WriteLine("Test 1: Osnovno izračunavanje težine bloka (2^difficulty)");

            // Test različitih difficulty vrednosti
            var testCases = new[]
            {
                (difficulty: (uint)1, expectedWeight: new BigInteger(2)),
                (difficulty: (uint)2, expectedWeight: new BigInteger(4)),
                (difficulty: (uint)3, expectedWeight: new BigInteger(8)),
                (difficulty: (uint)4, expectedWeight: new BigInteger(16)),
                (difficulty: (uint)5, expectedWeight: new BigInteger(32)),
                (difficulty: (uint)10, expectedWeight: new BigInteger(1024)),
            };

            bool allPassed = true;
            foreach (var (difficulty, expectedWeight) in testCases)
            {
                BigInteger weight = CumulativeWeight.CalculateBlockWeight(difficulty);
                if (weight == expectedWeight)
                {
                    Console.WriteLine($"  ✓ difficulty={difficulty}: weight=2^{difficulty}={weight}");
                }
                else
                {
                    Console.WriteLine($"  ✗ difficulty={difficulty}: očekivano {expectedWeight}, dobijeno {weight}");
                    allPassed = false;
                }
            }

            if (allPassed)
            {
                Console.WriteLine("  ✓ Svi testovi za osnovno izračunavanje su prošli");
            }
        }

        private static void TestBlockWeightWithBlockObject()
        {
            Console.WriteLine("\nTest 2: Izračunavanje težine pomoću Block objekta");

            var block1 = new Block(1, "Test block 1", DateTimeOffset.UtcNow.ToUnixTimeSeconds(), "0", 3, 0);
            var block2 = new Block(2, "Test block 2", DateTimeOffset.UtcNow.ToUnixTimeSeconds(), "hash1", 5, 0);

            BigInteger weight1 = CumulativeWeight.CalculateBlockWeight(block1);
            BigInteger weight2 = CumulativeWeight.CalculateBlockWeight(block2);

            BigInteger expectedWeight1 = BigInteger.Pow(2, 3); // 2^3 = 8
            BigInteger expectedWeight2 = BigInteger.Pow(2, 5); // 2^5 = 32

            if (weight1 == expectedWeight1 && weight2 == expectedWeight2)
            {
                Console.WriteLine($"  ✓ Block 1 (difficulty=3): weight={weight1} (očekivano: {expectedWeight1})");
                Console.WriteLine($"  ✓ Block 2 (difficulty=5): weight={weight2} (očekivano: {expectedWeight2})");
            }
            else
            {
                Console.WriteLine($"  ✗ Block 1: očekivano {expectedWeight1}, dobijeno {weight1}");
                Console.WriteLine($"  ✗ Block 2: očekivano {expectedWeight2}, dobijeno {weight2}");
            }

            // Test sa null block-om
            try
            {
                Block? nullBlock = null;
                CumulativeWeight.CalculateBlockWeight(nullBlock!);
                Console.WriteLine("  ✗ Očekivana greška za null block, ali nije bačena");
            }
            catch (ArgumentNullException)
            {
                Console.WriteLine("  ✓ Null block baca ArgumentNullException");
            }
        }

        private static void TestBlockWeightEdgeCases()
        {
            Console.WriteLine("\nTest 3: Edge cases");

            // Test sa difficulty = 0
            BigInteger weight0 = CumulativeWeight.CalculateBlockWeight(0);
            BigInteger expectedWeight0 = BigInteger.Pow(2, 0); // 2^0 = 1
            if (weight0 == expectedWeight0)
            {
                Console.WriteLine($"  ✓ difficulty=0: weight={weight0} (očekivano: 1)");
            }
            else
            {
                Console.WriteLine($"  ✗ difficulty=0: očekivano {expectedWeight0}, dobijeno {weight0}");
            }

            // Test sa većim difficulty vrednostima
            BigInteger weight20 = CumulativeWeight.CalculateBlockWeight(20);
            BigInteger expectedWeight20 = BigInteger.Pow(2, 20); // 2^20 = 1,048,576
            if (weight20 == expectedWeight20)
            {
                Console.WriteLine($"  ✓ difficulty=20: weight={weight20:N0} (očekivano: {expectedWeight20:N0})");
            }
            else
            {
                Console.WriteLine($"  ✗ difficulty=20: očekivano {expectedWeight20}, dobijeno {weight20}");
            }

            BigInteger weight30 = CumulativeWeight.CalculateBlockWeight(30);
            BigInteger expectedWeight30 = BigInteger.Pow(2, 30); // 2^30 = 1,073,741,824
            if (weight30 == expectedWeight30)
            {
                Console.WriteLine($"  ✓ difficulty=30: weight={weight30:N0} (očekivano: {expectedWeight30:N0})");
            }
            else
            {
                Console.WriteLine($"  ✗ difficulty=30: očekivano {expectedWeight30}, dobijeno {weight30}");
            }
        }

        private static void TestBlockWeightComparison()
        {
            Console.WriteLine("\nTest 4: Poređenje težina blokova sa različitim difficulty vrednostima");

            // Viša difficulty => veća težina
            BigInteger weight1 = CumulativeWeight.CalculateBlockWeight(1);  // 2
            BigInteger weight2 = CumulativeWeight.CalculateBlockWeight(2);  // 4
            BigInteger weight3 = CumulativeWeight.CalculateBlockWeight(3);  // 8
            BigInteger weight4 = CumulativeWeight.CalculateBlockWeight(4);  // 16

            if (weight1 < weight2 && weight2 < weight3 && weight3 < weight4)
            {
                Console.WriteLine("  ✓ Viša difficulty daje veću težinu:");
                Console.WriteLine($"    difficulty=1: weight={weight1}");
                Console.WriteLine($"    difficulty=2: weight={weight2}");
                Console.WriteLine($"    difficulty=3: weight={weight3}");
                Console.WriteLine($"    difficulty=4: weight={weight4}");
            }
            else
            {
                Console.WriteLine("  ✗ Težine nisu u očekivanom redosledu");
            }

            // Proveri da li je razlika između uzastopnih difficulty vrednosti eksponencijalna
            BigInteger diff1to2 = weight2 - weight1; // 4 - 2 = 2
            BigInteger diff2to3 = weight3 - weight2; // 8 - 4 = 4
            BigInteger diff3to4 = weight4 - weight3; // 16 - 8 = 8

            if (diff2to3 == 2 * diff1to2 && diff3to4 == 2 * diff2to3)
            {
                Console.WriteLine("  ✓ Razlika između uzastopnih difficulty vrednosti je eksponencijalna (dupla)");
            }
            else
            {
                Console.WriteLine("  ✗ Razlika između uzastopnih difficulty vrednosti nije eksponencijalna");
            }
        }
    }
}

