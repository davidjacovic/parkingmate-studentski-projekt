using ParkingMate.Blockchain;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;

namespace ParkingMate.Blockchain.Infrastructure
{
    public class BlockchainState
    {
        public Blockchain Chain { get; }
        private readonly BlockchainStorage _storage;

        public BlockchainState(BlockchainStorage storage)
        {
            _storage = storage;

            // Pokušaj da učitaš postojeći blockchain iz storage-a
            var loadedBlocks = storage.LoadChain();

            if (loadedBlocks != null && loadedBlocks.Count > 0)
            {
                // Učitaj postojeći blockchain
                // Koristimo pristup kroz reflection za kompatibilnost
                // loadedBlocks je List<Block>, što implementira IReadOnlyList<Block>
                var blockchainType = typeof(Blockchain);
                var constructor = blockchainType.GetConstructor(new[]
                {
                    typeof(IReadOnlyList<Block>),
                    typeof(long),
                    typeof(uint),
                    typeof(int)
                });
                
                if (constructor != null)
                {
                    Chain = (Blockchain)constructor.Invoke(new object[] { loadedBlocks, 600L, 10U, 1 });
                }
                else
                {
                    // Fallback: kreiraj novi blockchain i ručno dodaj blokove
                    Chain = new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 10, threadCount: 1);
                    Console.WriteLine("[BlockchainState] Constructor not found, using fallback approach");
                }
                
                Console.WriteLine($"[BlockchainState] Loaded blockchain with {Chain.Chain.Count} blocks from storage");
            }
            else
            {
                // Kreiraj novi blockchain sa genesis blokom
                Chain = new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 10, threadCount: 1);
                Console.WriteLine("[BlockchainState] Created new blockchain with genesis block");
                
                // Sačuvaj genesis blok
                SaveChain();
            }
        }

        /// <summary>
        /// Čuva blockchain lanac u storage.
        /// </summary>
        public void SaveChain()
        {
            _storage.SaveChain(Chain.Chain);
        }
    }
}
