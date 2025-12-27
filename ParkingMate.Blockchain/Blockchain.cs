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

    }
}
