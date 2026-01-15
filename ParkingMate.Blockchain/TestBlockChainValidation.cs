using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Funkcionalni test: validacija bloka i cele verige.
    /// Pokriva: "Validacijo blokov in verige" + integritetni uslovi iz teksta.
    /// </summary>
    public static class TestBlockChainValidation
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test Block & Chain Validation (funkcionalni) ===\n");

            TestValidChain();
            TestInvalidIndex();
            TestInvalidPreviousHash();
            TestInvalidStoredHash();
            TestInvalidDifficultyPrefix();

            Console.WriteLine("\n✓ TestBlockAndChainValidation završen.\n");
        }

        private static void TestValidChain()
        {
            Console.WriteLine("Test 1: Validan lanac");

            var bc = new Blockchain(threadCount: 1);

            // Dodaj 3 bloka – NE rudari ručno
            for (int i = 1; i <= 3; i++)
            {
                var block = new Block(
                    index: 0,              // ignoriše se
                    data: $"Valid block {i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "",      // ignoriše se
                    difficulty: 2,
                    nonce: 0
                );

                bc.AddBlock(block); // blockchain radi sve
            }

            bool chainOk = bc.IsValidChain();
            Console.WriteLine(chainOk
                ? "  ✓ Lanac je validan\n"
                : "  ✗ Lanac NIJE validan\n");
        }


        private static void TestInvalidIndex()
        {
            Console.WriteLine("Test 2: Nevalidan index (nije +1)");

            var bc = new Blockchain();
            var prev = bc.GetLatestBlock();

            var block = MineBlockSingleThread(
                index: prev.Index + 2, // namerno pogrešno
                data: "Bad index",
                previousHash: prev.Hash,
                difficulty: 2
            );

            bool ok = bc.IsValidNewBlock(block, prev);
            Console.WriteLine(!ok ? "  ✓ Očekivano odbijeno (loš index)\n" : "  ✗ Pogrešno prihvaćeno (loš index)\n");
        }

        private static void TestInvalidPreviousHash()
        {
            Console.WriteLine("Test 3: Nevalidan previousHash (ne poklapa se)");

            var bc = new Blockchain();
            var prev = bc.GetLatestBlock();

            var block = MineBlockSingleThread(
                index: prev.Index + 1,
                data: "Bad prev hash",
                previousHash: "NOT_THE_REAL_PREV_HASH",
                difficulty: 2
            );

            bool ok = bc.IsValidNewBlock(block, prev);
            Console.WriteLine(!ok ? "  ✓ Očekivano odbijeno (loš previousHash)\n" : "  ✗ Pogrešno prihvaćeno (loš previousHash)\n");
        }

        private static void TestInvalidStoredHash()
        {
            Console.WriteLine("Test 4: Nevalidan stored hash (Hash != CalculateHash)");

            var bc = new Blockchain();
            var prev = bc.GetLatestBlock();

            var block = MineBlockSingleThread(
                index: prev.Index + 1,
                data: "Tampered hash",
                previousHash: prev.Hash,
                difficulty: 2
            );

            // Namerno pokvari hash
            block.Hash = new string('a', Math.Max(1, block.Hash.Length));

            bool ok = bc.IsValidNewBlock(block, prev);
            Console.WriteLine(!ok ? "  ✓ Očekivano odbijeno (pokvaren stored hash)\n" : "  ✗ Pogrešno prihvaćeno (pokvaren stored hash)\n");
        }

        private static void TestInvalidDifficultyPrefix()
        {
            Console.WriteLine("Test 5: Nevalidan difficulty prefix (hash ne počinje sa 0...0)");

            var bc = new Blockchain();
            var prev = bc.GetLatestBlock();

            var block = new Block(
                index: prev.Index + 1,
                data: "No PoW",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: prev.Hash,
                difficulty: 3, // traži 000...
                nonce: 0
            );

            // Hash izračunaj, ali ne rudari (verovatno neće početi sa "000")
            block.Hash = block.CalculateHash();

            bool ok = bc.IsValidNewBlock(block, prev);
            Console.WriteLine(!ok ? "  ✓ Očekivano odbijeno (nema PoW)\n" : "  ✗ Pogrešno prihvaćeno (nema PoW)\n");
        }

        /// <summary>
        /// Validacija cele verige iteracijom: svaki blok mora biti validan prema prethodnom.
        /// (Ovo je fallback ako nemaš Blockchain.IsValidChain()).
        /// </summary>
        private static bool ValidateChainByIteration(IReadOnlyList<Block> chain)
        {
            if (chain == null || chain.Count == 0) return true;
            for (int i = 1; i < chain.Count; i++)
            {
                var prev = chain[i - 1];
                var cur = chain[i];

                // Osnovne provere iz specifikacije
                if (cur.Index != prev.Index + 1) return false;
                if (cur.PreviousHash != prev.Hash) return false;

                var calc = cur.CalculateHash();
                if (cur.Hash != calc) return false;

                string prefix = new string('0', (int)cur.Difficulty);
                if (!cur.Hash.StartsWith(prefix)) return false;
            }
            return true;
        }

        /// <summary>
        /// Minimalan single-thread PoW mining (za test).
        /// </summary>
        private static Block MineBlockSingleThread(uint index, string data, string previousHash, uint difficulty)
        {
            var b = new Block(
                index: index,
                data: data,
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: previousHash,
                difficulty: difficulty,
                nonce: 0
            );

            string prefix = new string('0', (int)difficulty);
            while (true)
            {
                b.Hash = b.CalculateHash();
                if (b.Hash.StartsWith(prefix)) return b;
                b.Nonce++;
            }
        }
    }
}
