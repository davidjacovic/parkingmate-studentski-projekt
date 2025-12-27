using System;

namespace ParkingMate.Blockchain
{
    class Program
    {
        static void Main(string[] args)
        {
            var block = new Block(
                1,
                "Test data",
                DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                "0",
                3,
                0
            );

            //Console.WriteLine(block.Serialize());
            //Console.WriteLine(block);

            string hash = block.CalculateHash();
            Console.WriteLine(hash);
        }
    }
}
