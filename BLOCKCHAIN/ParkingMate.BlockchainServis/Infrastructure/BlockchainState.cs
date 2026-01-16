using ParkingMate.Blockchain;

namespace ParkingMate.Blockchain.Infrastructure
{
    public class BlockchainState
    {
        public Blockchain Chain { get; }

        public BlockchainState()
        {
            Chain = new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 10, threadCount: 1);
        }
    }
}
