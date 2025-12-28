using System.Collections.Generic;
using System;


namespace ParkingMate.Blockchain
{
    public class Blockchain
    {
        private readonly List<Block> chain;

        public Blockchain()
        {
            chain = new List<Block>();
            chain.Add(CreateGenesisBlock());
        }

        public IReadOnlyList<Block> Chain => chain;

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
