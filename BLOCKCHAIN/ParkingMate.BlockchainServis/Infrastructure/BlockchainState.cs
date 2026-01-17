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
                var constructor = blockchainType.GetConstructor(BindingFlags.Public | BindingFlags.Instance, null, new[]
                {
                    typeof(IReadOnlyList<Block>),
                    typeof(long),
                    typeof(uint),
                    typeof(int)
                }, null);
                
                if (constructor != null)
                {
                    Chain = (Blockchain)constructor.Invoke(new object[] { loadedBlocks, 600L, 10U, 1 });
                    Console.WriteLine($"[BlockchainState] Loaded blockchain with {Chain.Chain.Count} blocks from storage (using constructor)");
                }
                else
                {
                    // Fallback: kreiraj novi blockchain BEZ genesis bloka i dodaj sve blokove ručno
                    // Kreiraj prazan blockchain i dodaj blokove jedan po jedan
                    Chain = CreateBlockchainWithBlocks(loadedBlocks);
                    Console.WriteLine($"[BlockchainState] Loaded blockchain with {Chain.Chain.Count} blocks from storage (using fallback approach)");
                }
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

        /// <summary>
        /// Kreira blockchain sa postojećim blokovima koristeći reflection pristup.
        /// </summary>
        private Blockchain CreateBlockchainWithBlocks(List<Block> blocks)
        {
            // Prvo validiraj da li su blokovi validni
            if (blocks == null || blocks.Count == 0)
            {
                return new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 10, threadCount: 1);
            }

            // Validiraj da li je prvi blok genesis blok
            if (blocks[0].Index != 0 || blocks[0].PreviousHash != "0")
            {
                Console.Error.WriteLine("[BlockchainState] ✗ Invalid blockchain: first block is not genesis block");
                return new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 10, threadCount: 1);
            }

            // Kreiraj novi blockchain instancu (sa genesis blokom)
            var chain = new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 10, threadCount: 1);
            
            // Koristimo reflection da zamenimo chain listu sa učitanim blokovima
            var chainField = typeof(Blockchain).GetField("chain", BindingFlags.NonPublic | BindingFlags.Instance | BindingFlags.IgnoreCase);
            if (chainField != null)
            {
                var chainList = chainField.GetValue(chain) as System.Collections.Generic.List<Block>;
                if (chainList != null)
                {
                    chainList.Clear();
                    chainList.AddRange(blocks);
                    Console.WriteLine($"[BlockchainState] Replaced chain list with {blocks.Count} blocks via reflection");
                }
            }
            
            // Fallback: ako reflection ne radi, koristimo AppendMinedBlock za sve blokove osim genesis
            if (chain.Chain.Count == 1) // Ako još uvek ima samo genesis blok
            {
                var appendMethod = typeof(Blockchain).GetMethod("AppendMinedBlock", BindingFlags.Public | BindingFlags.Instance);
                if (appendMethod != null)
                {
                    // Dodaj sve blokove osim genesis (koji već postoji)
                    for (int i = 1; i < blocks.Count; i++)
                    {
                        appendMethod.Invoke(chain, new object[] { blocks[i] });
                    }
                    Console.WriteLine($"[BlockchainState] Added {blocks.Count - 1} blocks via AppendMinedBlock");
                }
            }

            return chain;
        }
    }
}
