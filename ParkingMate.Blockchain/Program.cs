using System;

namespace ParkingMate.Blockchain
{
    class Program
    {
        static void Main()
        {
            var blockchain = new Blockchain();

            blockchain.AddBlock(new Block(
                0,
                "First real block",
                DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                "",
                2,
                0
            ));

            Console.WriteLine("=== Blockchain contents ===");

            foreach (var block in blockchain.Chain)
            {
                Console.WriteLine(block);
            }

            Console.WriteLine("=== Structural checks ===");
            Console.WriteLine($"Block count: {blockchain.Chain.Count}");
            Console.WriteLine($"Genesis previousHash: {blockchain.Chain[0].PreviousHash}");
            Console.WriteLine($"Second block previousHash == genesis hash: " +
                $"{blockchain.Chain[1].PreviousHash == blockchain.Chain[0].Hash}");
        }
    }
}
