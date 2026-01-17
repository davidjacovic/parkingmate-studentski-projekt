using System;
using System.Collections.Generic;
using System.Diagnostics;

namespace ParkingMate.Blockchain
{
    public class Blockchain
    {
        private readonly List<Block> chain;
        private readonly long blockIntervalSeconds;
        private readonly uint adjustmentInterval;
        private readonly int threadCount;

        public Blockchain(long blockIntervalSeconds = 600, uint adjustmentInterval = 10, int threadCount = 1)
        {
            this.blockIntervalSeconds = blockIntervalSeconds;
            this.adjustmentInterval = adjustmentInterval;

            if (threadCount == 0)
                this.threadCount = ThreadedMiner.GetOptimalThreadCount();
            else if (threadCount < 1)
                throw new ArgumentException("threadCount mora biti >= 1 ili 0 za automatsku detekciju", nameof(threadCount));
            else
                this.threadCount = threadCount;

            chain = new List<Block>();
            chain.Add(CreateGenesisBlock());
        }

        /// <summary>
        /// Kreira Blockchain instancu iz postojeće liste blokova (za učitavanje iz storage-a).
        /// </summary>
        public Blockchain(IReadOnlyList<Block> existingBlocks, long blockIntervalSeconds = 600, uint adjustmentInterval = 10, int threadCount = 1)
        {
            this.blockIntervalSeconds = blockIntervalSeconds;
            this.adjustmentInterval = adjustmentInterval;

            if (threadCount == 0)
                this.threadCount = ThreadedMiner.GetOptimalThreadCount();
            else if (threadCount < 1)
                throw new ArgumentException("threadCount mora biti >= 1 ili 0 za automatsku detekciju", nameof(threadCount));
            else
                this.threadCount = threadCount;

            if (existingBlocks == null || existingBlocks.Count == 0)
            {
                chain = new List<Block>();
                chain.Add(CreateGenesisBlock());
            }
            else
            {
                // Prvo inicijalizuj chain, pa onda validiraj
                chain = new List<Block>(existingBlocks);
                
                // Validiraj lanac pre nego što ga koristimo
                if (!IsValidChain())
                {
                    throw new ArgumentException("Invalid blockchain chain provided - chain validation failed", nameof(existingBlocks));
                }
            }
        }

        public IReadOnlyList<Block> Chain => chain;
        public long BlockIntervalSeconds => blockIntervalSeconds;
        public uint AdjustmentInterval => adjustmentInterval;
        public int ThreadCount => threadCount;

        private Block CreateGenesisBlock()
        {
            var genesis = new Block(
                index: 0,
                data: "Genesis Block",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: 1,
                nonce: 0
            );
            genesis.Hash = genesis.CalculateHash();
            return genesis;
        }

        public Block GetLatestBlock() => chain[^1];

        public uint GetNextDifficulty()
        {
            var latest = GetLatestBlock();

            if (!DynamicDifficulty.ShouldAdjustDifficulty(chain.Count, adjustmentInterval))
                return latest.Difficulty;

            return DynamicDifficulty.CalculateDifficulty(chain, latest.Difficulty, blockIntervalSeconds, adjustmentInterval);
        }


        // ✅ koristi se u MPI režimu (master dobije već mined block)
        public void AppendMinedBlock(Block mined)
        {
            chain.Add(mined);
        }

        public void AddBlock(Block newBlock)
        {
            var latest = GetLatestBlock();

            uint nextIndex = latest.Index + 1;
            string previousHash = latest.Hash;
            long finalTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();

            // ✅ difficulty mora doći iz dinamičkog algoritma
            uint nextDifficulty = GetNextDifficulty();

            var blockToMine = new Block(
                index: nextIndex,
                data: newBlock.Data,
                timestamp: finalTimestamp,
                previousHash: previousHash,
                difficulty: nextDifficulty,
                nonce: 0
            );

            Block minedBlock;
            long miningTimeMs;

            if (threadCount > 1)
                minedBlock = MineBlockMultiThreaded(blockToMine, out miningTimeMs);
            else
                minedBlock = MineBlockSingleThreaded(blockToMine, out miningTimeMs);

            Console.WriteLine($"Mining time: {miningTimeMs} ms (threads: {threadCount})");

            if (!IsValidNewBlock(minedBlock, latest))
                throw new InvalidOperationException("Invalid mined block");

            chain.Add(minedBlock);
        }


        private Block MineBlockSingleThreaded(Block block, out long miningTimeMs)
        {
            block.Nonce = 0;
            string prefix = new string('0', (int)block.Difficulty);

            var sw = Stopwatch.StartNew();
            while (true)
            {
                block.Hash = block.CalculateHash();
                if (block.Hash.StartsWith(prefix))
                    break;
                block.Nonce++;
            }
            sw.Stop();
            miningTimeMs = sw.ElapsedMilliseconds;

            return block;
        }

        private Block MineBlockMultiThreaded(Block blockToMine, out long miningTimeMs)
        {
            var sw = Stopwatch.StartNew();

            var pool = new ThreadedMiner.MiningThreadPool(threadCount);
            var sharedState = pool.SharedState;

            var workers = new List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < threadCount; i++)
            {
                var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(threadCount, i);

                var copy = new Block(
                    blockToMine.Index,
                    blockToMine.Data,
                    blockToMine.Timestamp,
                    blockToMine.PreviousHash,
                    blockToMine.Difficulty,
                    0
                );

                workers.Add(new ThreadedMiner.BlockMiningWorker(i, sharedState, copy, startNonce, endNonce));
            }

            pool.StartWithWorkers(workers);
            pool.WaitAll();

            var found = sharedState.GetFoundBlock();
            if (found == null)
            {
                pool.Stop();
                throw new InvalidOperationException("Mining failed: no solution found in nonce range");
            }

            sw.Stop();
            miningTimeMs = sw.ElapsedMilliseconds;

            pool.Stop();

            blockToMine.Nonce = found.Nonce;
            blockToMine.Hash = found.Hash;

            return blockToMine;
        }

        public bool IsValidNewBlock(Block current, Block previous)
        {
            if (current.Index != previous.Index + 1) return false;
            if (current.PreviousHash != previous.Hash) return false;

            string calculatedHash = current.CalculateHash();
            if (current.Hash != calculatedHash) return false;

            string prefix = new string('0', (int)current.Difficulty);
            if (!current.Hash.StartsWith(prefix)) return false;

            if (!TimestampValidator.IsValidTimestamp(current, previous)) return false;

            return true;
        }

        public bool IsValidChain()
        {
            if (chain.Count == 0) return false;

            for (int i = 1; i < chain.Count; i++)
                if (!IsValidNewBlock(chain[i], chain[i - 1]))
                    return false;

            return true;
        }

        public bool IsValidChain(IReadOnlyList<Block> candidateChain)
        {
            if (candidateChain == null || candidateChain.Count == 0) return false;

            if (candidateChain[0].PreviousHash != "0") return false;
            if (candidateChain[0].Hash != candidateChain[0].CalculateHash()) return false;

            for (int i = 1; i < candidateChain.Count; i++)
                if (!IsValidNewBlock(candidateChain[i], candidateChain[i - 1]))
                    return false;

            return true;
        }

        public bool TryReplaceChain(IReadOnlyList<Block> candidateChain)
        {
            if (!IsValidChain(candidateChain)) return false;

            var currentWeight = CumulativeWeight.CalculateChainWeight(this.Chain);
            var candidateWeight = CumulativeWeight.CalculateChainWeight(candidateChain);

            if (candidateWeight > currentWeight)
            {
                chain.Clear();
                chain.AddRange(candidateChain);
                return true;
            }

            return false;
        }
    }
}
