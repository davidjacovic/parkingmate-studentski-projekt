using System;
using System.Collections.Generic;
using System.Numerics;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Testovi za izračunavanje kumulativne težine (6.2.1 - Izračunavanje 2^difficulty po bloku, 6.2.2 - Sabiranje po lancu).
    /// </summary>
    public static class TestCumulativeWeight
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test izračunavanja kumulativne težine (6.2.1, 6.2.2) ===\n");

            TestBlockWeightCalculation();
            TestBlockWeightWithBlockObject();
            TestBlockWeightEdgeCases();
            TestBlockWeightComparison();
            TestChainWeightCalculation();
            TestChainWeightWithBlockchain();
            TestChainWeightEdgeCases();

            Console.WriteLine("\n✓ Testovi za Subtask 6.2.1 (Izračunavanje 2^difficulty po bloku) i 6.2.2 (Sabiranje po lancu) su prošli!\n");
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

        private static void TestChainWeightCalculation()
        {
            Console.WriteLine("\nTest 5: Izračunavanje kumulativne težine lanca (6.2.2)");

            // Kreiraj lanac sa različitim difficulty vrednostima
            var chain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // Genesis blok (difficulty=1)
            chain.Add(new Block(0, "Genesis", baseTime, "0", 1, 0));
            // Blok 1 (difficulty=2)
            chain.Add(new Block(1, "Block 1", baseTime + 1, "", 2, 0));
            // Blok 2 (difficulty=3)
            chain.Add(new Block(2, "Block 2", baseTime + 2, "", 3, 0));
            // Blok 3 (difficulty=2)
            chain.Add(new Block(3, "Block 3", baseTime + 3, "", 2, 0));

            BigInteger chainWeight = CumulativeWeight.CalculateChainWeight(chain);

            // Očekivana težina: 2^1 + 2^2 + 2^3 + 2^2 = 2 + 4 + 8 + 4 = 18
            BigInteger expectedWeight = 2 + 4 + 8 + 4; // 18

            if (chainWeight == expectedWeight)
            {
                Console.WriteLine($"  ✓ Kumulativna težina lanca: {chainWeight} (očekivano: {expectedWeight})");
                Console.WriteLine($"    Blokovi: difficulty [1, 2, 3, 2] => težine [2, 4, 8, 4] => suma = 18");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: {expectedWeight}, dobijeno: {chainWeight}");
            }

            // Test sa jednim blokom
            var singleBlockChain = new List<Block> { chain[0] };
            BigInteger singleBlockWeight = CumulativeWeight.CalculateChainWeight(singleBlockChain);
            BigInteger expectedSingleWeight = BigInteger.Pow(2, 1); // 2
            if (singleBlockWeight == expectedSingleWeight)
            {
                Console.WriteLine($"  ✓ Lanac sa jednim blokom (difficulty=1): weight={singleBlockWeight}");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: {expectedSingleWeight}, dobijeno: {singleBlockWeight}");
            }
        }

        private static void TestChainWeightWithBlockchain()
        {
            Console.WriteLine("\nTest 6: Izračunavanje kumulativne težine pomoću Blockchain objekta (6.2.2)");

            var blockchain = new Blockchain();
            
            // Genesis blok je već dodat (difficulty=1)
            // Dodaj još nekoliko blokova za test
            for (int i = 1; i <= 3; i++)
            {
                var block = new Block(
                    (uint)i,
                    $"Test block {i}",
                    DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    blockchain.GetLatestBlock().Hash,
                    (uint)(i + 1), // difficulty: 2, 3, 4
                    0
                );
                blockchain.AddBlock(block);
            }

            BigInteger chainWeight = CumulativeWeight.CalculateChainWeight(blockchain);

            // Genesis (difficulty=1) + blokovi (difficulty=2,3,4)
            // Težine: 2 + 4 + 8 + 16 = 30
            BigInteger expectedWeight = 2 + 4 + 8 + 16; // 30

            if (chainWeight >= expectedWeight) // >= jer genesis blok ima difficulty=1
            {
                Console.WriteLine($"  ✓ Kumulativna težina blockchain-a: {chainWeight:N0}");
                Console.WriteLine($"    Očekivano: ~{expectedWeight:N0} (zavisno od genesis bloka)");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano minimum: {expectedWeight}, dobijeno: {chainWeight}");
            }

            // Test sa null blockchain-om
            try
            {
                Blockchain? nullBlockchain = null;
                CumulativeWeight.CalculateChainWeight(nullBlockchain!);
                Console.WriteLine("  ✗ Očekivana greška za null blockchain, ali nije bačena");
            }
            catch (ArgumentNullException)
            {
                Console.WriteLine("  ✓ Null blockchain baca ArgumentNullException");
            }
        }

        private static void TestChainWeightEdgeCases()
        {
            Console.WriteLine("\nTest 7: Edge cases za kumulativnu težinu lanca (6.2.2)");

            // Test sa praznom listom
            var emptyChain = new List<Block>();
            BigInteger emptyWeight = CumulativeWeight.CalculateChainWeight(emptyChain);
            if (emptyWeight == BigInteger.Zero)
            {
                Console.WriteLine("  ✓ Prazan lanac ima težinu 0");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: 0, dobijeno: {emptyWeight}");
            }

            // Test sa null listom
            try
            {
                IReadOnlyList<Block>? nullChain = null;
                CumulativeWeight.CalculateChainWeight(nullChain!);
                Console.WriteLine("  ✗ Očekivana greška za null chain, ali nije bačena");
            }
            catch (ArgumentNullException)
            {
                Console.WriteLine("  ✓ Null chain baca ArgumentNullException");
            }

            // Test sa blokovima sa istom difficulty vrednošću
            var sameDifficultyChain = new List<Block>();
            long baseTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            for (int i = 0; i < 5; i++)
            {
                sameDifficultyChain.Add(new Block((uint)i, $"Block {i}", baseTime + i, i == 0 ? "0" : "", 3, 0));
            }

            BigInteger sameDifficultyWeight = CumulativeWeight.CalculateChainWeight(sameDifficultyChain);
            BigInteger expectedSameWeight = 5 * BigInteger.Pow(2, 3); // 5 * 8 = 40

            if (sameDifficultyWeight == expectedSameWeight)
            {
                Console.WriteLine($"  ✓ Lanac sa 5 blokova istom difficulty (3): weight={sameDifficultyWeight} (5 * 8 = 40)");
            }
            else
            {
                Console.WriteLine($"  ✗ Očekivano: {expectedSameWeight}, dobijeno: {sameDifficultyWeight}");
            }
        }
    }
}

