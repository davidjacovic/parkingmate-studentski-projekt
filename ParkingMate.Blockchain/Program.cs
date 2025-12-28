using System;

namespace ParkingMate.Blockchain
{
    class Program
    {
        static void Main()
        {
            var blockchain = new Blockchain();

            int blocksToMine = 30;

            for (int i = 1; i <= blocksToMine; i++)
            {
                var block = new Block(
                    index: 0,
                    data: $"Auto-mined block #{i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "",
                    difficulty: 3,
                    nonce: 0
                );

                Console.WriteLine($"\nMining block {i}...");
                blockchain.AddBlock(block);
            }

            Console.WriteLine("\n=== Final Blockchain ===");
            foreach (var block in blockchain.Chain)
            {
                Console.WriteLine(block);
            }

            Console.WriteLine("Blockchain valid: " + blockchain.IsValidChain());
        }
    }
}
