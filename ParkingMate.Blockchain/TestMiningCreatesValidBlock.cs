using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Funkcionalni/integracioni test: multi-thread mining mora da napravi VALIDAN blok.
    /// Pokriva: "Ustvarjanje novih blokov" + "integriteta" (IsValidNewBlock).
    /// </summary>
    public static class TestMiningCreatesValidBlock
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test Mining Creates Valid Block (funkcionalni/integracioni) ===\n");

            var bc = new Blockchain();
            var prev = bc.GetLatestBlock();

            uint difficulty = 3; // po potrebi spusti na 2 ako ti je sporije
            int threads = Math.Min(4, ThreadedMiner.GetAvailableProcessorCount());

            Console.WriteLine($"Koristim threads={threads}, difficulty={difficulty}");

            var candidate = new Block(
                index: prev.Index + 1,
                data: "Mined block test",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: prev.Hash,
                difficulty: difficulty,
                nonce: 0
            );

            var mined = MineWithThreads(candidate, threads);

            if (mined == null)
            {
                Console.WriteLine("  ✗ Nije pronađen blok (mining vratio null).");
                return;
            }

            // 1) Proveri PoW prefix
            string prefix = new string('0', (int)mined.Difficulty);
            if (!mined.Hash.StartsWith(prefix))
            {
                Console.WriteLine("  ✗ Hash nema potreban prefix nula (PoW neuspeh).");
                Console.WriteLine($"    Hash: {mined.Hash}");
                return;
            }

            // 2) Proveri stored hash = CalculateHash
            var calc = mined.CalculateHash();
            if (calc != mined.Hash)
            {
                Console.WriteLine("  ✗ Stored hash != CalculateHash()");
                Console.WriteLine($"    Stored: {mined.Hash}");
                Console.WriteLine($"    Calc:   {calc}");
                return;
            }

            // 3) Proveri Blockchain.IsValidNewBlock
            bool ok = bc.IsValidNewBlock(mined, prev);
            if (!ok)
            {
                Console.WriteLine("  ✗ Blockchain.IsValidNewBlock odbacuje iskopani blok.");
                return;
            }

            // 4) Dodaj i proveri da je lanac porastao
            int before = bc.Chain.Count;
            bc.AddBlock(mined);
            int after = bc.Chain.Count;

            if (after == before + 1)
            {
                Console.WriteLine("  ✓ Mining je napravio validan blok i uspešno je dodat u lanac.");
                Console.WriteLine($"    Nonce: {mined.Nonce:N0}");
                Console.WriteLine($"    Hash:  {mined.Hash.Substring(0, Math.Min(24, mined.Hash.Length))}...");
            }
            else
            {
                Console.WriteLine("  ⚠ Blok je validan, ali AddBlock nije povećao lanac (proveri AddBlock logiku).");
            }

            Console.WriteLine();
        }

        private static Block? MineWithThreads(Block blockToMine, int threadCount)
        {
            var pool = new ThreadedMiner.MiningThreadPool(threadCount);
            var shared = pool.SharedState;

            var workers = new List<ThreadedMiner.MiningWorker>(threadCount);
            for (int i = 0; i < threadCount; i++)
            {
                var (start, end) = ThreadedMiner.CalculateNonceRange(threadCount, i);

                // Kopija bloka po niti (kao kod tebe u TestScaling)
                var copy = new Block(
                    blockToMine.Index,
                    blockToMine.Data,
                    blockToMine.Timestamp,
                    blockToMine.PreviousHash,
                    blockToMine.Difficulty,
                    blockToMine.Nonce
                );

                var w = new ThreadedMiner.BlockMiningWorker(
                    threadId: i,
                    sharedState: shared,
                    blockToMine: copy,
                    startNonce: start,
                    endNonce: end
                );
                workers.Add(w);
            }

            pool.StartWithWorkers(workers);
            pool.WaitAll();
            var found = shared.GetFoundBlock();
            pool.Stop();

            return found;
        }
    }
}
