using System.Collections.Generic;
using System;


namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Blockchain klasa sa integracijom dinamičke difficulty (6.1.3).
    /// </summary>
    public class Blockchain
    {
        private readonly List<Block> chain;
        private readonly long blockIntervalSeconds;
        private readonly uint adjustmentInterval;

        /// <summary>
        /// Konstruktor za Blockchain sa podrškom za dinamičku difficulty (6.1.3).
        /// </summary>
        /// <param name="blockIntervalSeconds">Ciljano vreme između blokova u sekundama (default: 600)</param>
        /// <param name="adjustmentInterval">Broj blokova nakon kojih se prilagođava difficulty (default: 10)</param>
        public Blockchain(long blockIntervalSeconds = 600, uint adjustmentInterval = 10)
        {
            this.blockIntervalSeconds = blockIntervalSeconds;
            this.adjustmentInterval = adjustmentInterval;
            chain = new List<Block>();
            chain.Add(CreateGenesisBlock());
        }

        public IReadOnlyList<Block> Chain => chain;
        
        /// <summary>
        /// Vraća block interval u sekundama (6.1.3).
        /// </summary>
        public long BlockIntervalSeconds => blockIntervalSeconds;
        
        /// <summary>
        /// Vraća adjustment interval (6.1.3).
        /// </summary>
        public uint AdjustmentInterval => adjustmentInterval;

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

        public Block GetLatestBlock()
        {
            return chain[^1];
        }

        /// <summary>
        /// Izračunava difficulty za sledeći blok koristeći dinamičku difficulty logiku (6.1.3).
        /// </summary>
        /// <param name="currentDifficulty">Trenutna difficulty vrednost</param>
        /// <returns>Nova difficulty vrednost za sledeći blok</returns>
        public uint GetNextDifficulty(uint currentDifficulty)
        {
            // Proveri da li je vreme za prilagođavanje difficulty-ja
            if (!DynamicDifficulty.ShouldAdjustDifficulty(chain.Count, adjustmentInterval))
            {
                // Nije vreme za prilagođavanje, vraća trenutnu difficulty
                return currentDifficulty;
            }

            // Prilagođi difficulty koristeći time-based algoritam
            return DynamicDifficulty.CalculateDifficulty(
                chain,
                currentDifficulty,
                blockIntervalSeconds,
                adjustmentInterval
            );
        }

        public void AddBlock(Block newBlock)
        {
            var latest = GetLatestBlock();

            newBlock.Index = latest.Index + 1;
            newBlock.PreviousHash = latest.Hash;

            newBlock.Nonce = 0;
            string prefix = new string('0', (int)newBlock.Difficulty);

            long startTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            while (true)
            {
                newBlock.Hash = newBlock.CalculateHash();
                if (newBlock.Hash.StartsWith(prefix))
                    break;
                newBlock.Nonce++;

            }
            long endTime = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            long miningTimeMs = endTime - startTime;
            Console.WriteLine($"Mining time: {miningTimeMs} ms");

            if (!IsValidNewBlock(newBlock, latest))
                throw new InvalidOperationException("Invalid mined block");

            chain.Add(newBlock);
        }


        public bool IsValidNewBlock(Block current, Block previous)
        {
            if (current.Index != previous.Index + 1)
                return false;

            if (current.PreviousHash != previous.Hash)
                return false;

            if (current.Hash != current.CalculateHash())
                return false;

            string prefix = new string('0', (int)current.Difficulty);
            if (!current.Hash.StartsWith(prefix))
                return false;

            return true;
        }
        public bool IsValidChain()
        {
            if (chain.Count == 0)
                return false;

            for (int i = 1; i < chain.Count; i++)
            {
                Block current = chain[i];
                Block previous = chain[i - 1];

                if (!IsValidNewBlock(current, previous))
                    return false;
            }

            return true;
        }

    }
}
