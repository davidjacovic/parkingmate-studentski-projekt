using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    public class Blockchain
    {
        private readonly List<Block> chain;

        public Blockchain()
        {
            chain = new List<Block>();
        }

        public IReadOnlyList<Block> Chain => chain;
    }
}
