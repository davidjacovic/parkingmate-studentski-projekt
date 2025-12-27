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
            newBlock.Hash = newBlock.CalculateHash();
<<<<<<< HEAD

            chain.Add(newBlock);
        }

    }
=======
            chain.Add(newBlock);

>>>>>>> 9aa9dd8 (PAR-335-PAR-335-Implementacija-strukture-blockchaina in dodan testni primer ta task)
}
