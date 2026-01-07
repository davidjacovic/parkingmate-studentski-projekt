using System.Collections.Generic;
using System;
using System.Diagnostics;


namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Blockchain klasa sa integracijom dinamičke difficulty (6.1.3) i multi-threaded mining-a (EPIC 4).
    /// </summary>
    public class Blockchain
    {
        private readonly List<Block> chain;
        private readonly long blockIntervalSeconds;
        private readonly uint adjustmentInterval;
        private readonly int threadCount;

        /// <summary>
        /// Konstruktor za Blockchain sa podrškom za dinamičku difficulty (6.1.3) i multi-threaded mining (EPIC 4).
        /// </summary>
        /// <param name="blockIntervalSeconds">Ciljano vreme između blokova u sekundama (default: 600)</param>
        /// <param name="adjustmentInterval">Broj blokova nakon kojih se prilagođava difficulty (default: 10)</param>
        /// <param name="threadCount">Broj niti za mining (default: 1 = single-threaded, ili automatska detekcija ako je 0)</param>
        public Blockchain(long blockIntervalSeconds = 600, uint adjustmentInterval = 10, int threadCount = 1)
        {
            this.blockIntervalSeconds = blockIntervalSeconds;
            this.adjustmentInterval = adjustmentInterval;
            
            // Ako je threadCount 0, koristi automatsku detekciju
            if (threadCount == 0)
            {
                this.threadCount = ThreadedMiner.GetOptimalThreadCount();
            }
            else if (threadCount < 1)
            {
                throw new ArgumentException("threadCount mora biti >= 1 ili 0 za automatsku detekciju", nameof(threadCount));
            }
            else
            {
                this.threadCount = threadCount;
            }
            
            chain = new List<Block>();
            chain.Add(CreateGenesisBlock());
        }

        public IReadOnlyList<Block> Chain => chain;
        
        /// <summary>
        /// Vraća block interval u sekundama (6.1.3).
        /// </summary>
        public long BlockIntervalSeconds => blockIntervalSeconds;
        
        /// <summary>
        /// Vraća adjustment interval (6.1.3).
        /// </summary>
        public uint AdjustmentInterval => adjustmentInterval;

        /// <summary>
        /// Vraća broj niti za mining (EPIC 4).
        /// </summary>
        public int ThreadCount => threadCount;

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

        /// <summary>
        /// Izračunava difficulty za sledeći blok koristeći dinamičku difficulty logiku (6.1.3).
        /// </summary>
        /// <param name="currentDifficulty">Trenutna difficulty vrednost</param>
        /// <returns>Nova difficulty vrednost za sledeći blok</returns>
        public uint GetNextDifficulty(uint currentDifficulty)
        {
            // Proveri da li je vreme za prilagođavanje difficulty-ja
            if (!DynamicDifficulty.ShouldAdjustDifficulty(chain.Count, adjustmentInterval))
            {
                // Nije vreme za prilagođavanje, vraća trenutnu difficulty
                return currentDifficulty;
            }

            // Prilagođi difficulty koristeći time-based algoritam
            return DynamicDifficulty.CalculateDifficulty(
                chain,
                currentDifficulty,
                blockIntervalSeconds,
                adjustmentInterval
            );
        }

        /// <summary>
        /// Dodaje novi blok u blockchain. Koristi multi-threaded mining ako je threadCount > 1 (EPIC 4).
        /// </summary>
        /// <param name="newBlock">Blok koji se dodaje. Index, PreviousHash, Timestamp, i Difficulty se postavljaju automatski.</param>
        public void AddBlock(Block newBlock)
        {
            var latest = GetLatestBlock();

            // Postavi osnovne podatke bloka
            uint nextIndex = latest.Index + 1;
            string previousHash = latest.Hash;
            
            // Osiguraj da timestamp bude veći od prethodnog bloka (timestamp je readonly, moramo kreirati novi blok)
            // Timestamp mora biti postavljen pre mining-a jer je deo hash-a
            long blockTimestamp = newBlock.Timestamp;
            long minTimestamp = latest.Timestamp + 1; // Minimum timestamp mora biti veći od prethodnog
            long currentTimestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            
            // Koristi veći od: korisnički postavljen timestamp, minimum (previous + 1), ili trenutno vreme
            long finalTimestamp = Math.Max(Math.Max(blockTimestamp, minTimestamp), currentTimestamp);
            
            // Kreiramo novi blok sa ispravnim vrednostima (timestamp je readonly, moramo kreirati novi)
            Block blockToMine = new Block(
                index: nextIndex,
                data: newBlock.Data,
                timestamp: finalTimestamp,
                previousHash: previousHash,
                difficulty: newBlock.Difficulty,
                nonce: 0 // Počinje od 0 za mining
            );

            // EPIC 4: Multi-threaded mining integracija
            Block minedBlock;
            long miningTimeMs;
            
            if (threadCount > 1)
            {
                // Multi-threaded mining
                minedBlock = MineBlockMultiThreaded(blockToMine, out miningTimeMs);
            }
            else
            {
                // Single-threaded mining (backward compatibility)
                minedBlock = MineBlockSingleThreaded(blockToMine, out miningTimeMs);
            }

            Console.WriteLine($"Mining time: {miningTimeMs} ms (threads: {threadCount})");

            // Debug: Prikaži detalje bloka pre validacije
            string hashPreview = minedBlock.Hash != null && minedBlock.Hash.Length > 0 
                ? minedBlock.Hash.Substring(0, Math.Min(16, minedBlock.Hash.Length)) 
                : "null";
            Console.WriteLine($"[DEBUG] Mined block - Index: {minedBlock.Index}, Nonce: {minedBlock.Nonce}, Hash: {hashPreview}...");

            if (!IsValidNewBlock(minedBlock, latest))
            {
                Console.WriteLine($"[ERROR] Invalid mined block:");
                Console.WriteLine($"  Current: Index={minedBlock.Index}, Hash={minedBlock.Hash}, PreviousHash={minedBlock.PreviousHash}");
                Console.WriteLine($"  Previous: Index={latest.Index}, Hash={latest.Hash}");
                throw new InvalidOperationException("Invalid mined block");
            }

            chain.Add(minedBlock);
        }

        /// <summary>
        /// Single-threaded mining (EPIC 4 - backward compatibility).
        /// </summary>
        private Block MineBlockSingleThreaded(Block block, out long miningTimeMs)
        {
            block.Nonce = 0;
            string prefix = new string('0', (int)block.Difficulty);

            var stopwatch = Stopwatch.StartNew();
            while (true)
            {
                block.Hash = block.CalculateHash();
                if (block.Hash.StartsWith(prefix))
                    break;
                block.Nonce++;
            }
            stopwatch.Stop();
            miningTimeMs = stopwatch.ElapsedMilliseconds;

            return block;
        }

        /// <summary>
        /// Multi-threaded mining koristeći MiningThreadPool (EPIC 4).
        /// </summary>
        private Block MineBlockMultiThreaded(Block blockToMine, out long miningTimeMs)
        {
            var stopwatch = Stopwatch.StartNew();
            
            // Kreiraj ThreadPool
            var pool = new ThreadedMiner.MiningThreadPool(threadCount);
            var sharedState = pool.SharedState;

            // Kreiraj workers za svaku nit
            var workers = new List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < threadCount; i++)
            {
                var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(threadCount, i);
                
                // Napravi kopiju bloka za svaku nit
                // Napomena: Svaki worker će raditi sa svojom kopijom, ali će svi deliti isti shared state
                var blockCopy = new Block(
                    blockToMine.Index,
                    blockToMine.Data,
                    blockToMine.Timestamp,
                    blockToMine.PreviousHash,
                    blockToMine.Difficulty,
                    blockToMine.Nonce
                );

                var worker = new ThreadedMiner.BlockMiningWorker(
                    threadId: i,
                    sharedState: sharedState,
                    blockToMine: blockCopy,
                    startNonce: startNonce,
                    endNonce: endNonce
                );

                workers.Add(worker);
            }

            // Pokreni mining
            pool.StartWithWorkers(workers);
            
            // Čekaj da se mining završi
            pool.WaitAll();

            // Uzmi pronađeni blok
            Block? foundBlock = sharedState.GetFoundBlock();
            if (foundBlock == null)
            {
                pool.Stop();
                throw new InvalidOperationException("Mining failed: No valid nonce found in the nonce range");
            }

            stopwatch.Stop();
            miningTimeMs = stopwatch.ElapsedMilliseconds;
            
            // Cleanup
            pool.Stop();

            // Ažuriraj originalni blok sa pronađenim nonce-om i hash-om
            // Važno: Koristimo hash iz foundBlock jer je već validan i izračunat sa pravilnim nonce-om
            blockToMine.Nonce = foundBlock.Nonce;
            blockToMine.Hash = foundBlock.Hash;
            
            // Proveri da li su Index i PreviousHash jednaki (mogu biti različiti ako su se promenili)
            if (blockToMine.Index != foundBlock.Index || blockToMine.PreviousHash != foundBlock.PreviousHash)
            {
                // Ako su različiti, hash mora biti ponovo izračunat sa tačnim vrednostima
                // Ali pošto smo već postavili Nonce, samo treba ponovo izračunati hash
                blockToMine.Hash = blockToMine.CalculateHash();
                
                // Proveri da li je hash i dalje validan
                string prefix = new string('0', (int)blockToMine.Difficulty);
                if (!blockToMine.Hash.StartsWith(prefix))
                {
                    // Hash nije validan sa novim vrednostima - koristi hash iz foundBlock
                    // Ovo bi trebalo da bude retka situacija
                    Console.WriteLine($"[WARNING] Hash ne odgovara difficulty nakon ažuriranja Index/PreviousHash!");
                    string foundHashPreview = foundBlock.Hash != null && foundBlock.Hash.Length > 0 
                        ? foundBlock.Hash.Substring(0, Math.Min(16, foundBlock.Hash.Length)) 
                        : "null";
                    string blockHashPreview = blockToMine.Hash != null && blockToMine.Hash.Length > 0 
                        ? blockToMine.Hash.Substring(0, Math.Min(16, blockToMine.Hash.Length)) 
                        : "null";
                    Console.WriteLine($"  Original foundBlock: Index={foundBlock.Index}, PreviousHash={foundBlock.PreviousHash}, Hash={foundHashPreview}");
                    Console.WriteLine($"  Updated blockToMine: Index={blockToMine.Index}, PreviousHash={blockToMine.PreviousHash}, Hash={blockHashPreview}");
                    blockToMine.Hash = foundBlock.Hash ?? string.Empty;
                }
            }

            // Vrati blok sa ažuriranim nonce i hash-om
            return blockToMine;
        }


        public bool IsValidNewBlock(Block current, Block previous)
        {
            if (current.Index != previous.Index + 1)
            {
                Console.WriteLine($"[DEBUG] Invalid Index: current.Index={current.Index}, previous.Index+1={previous.Index + 1}");
                return false;
            }

            if (current.PreviousHash != previous.Hash)
            {
                Console.WriteLine($"[DEBUG] Invalid PreviousHash: current.PreviousHash={current.PreviousHash}, previous.Hash={previous.Hash}");
                return false;
            }

            string calculatedHash = current.CalculateHash();
            if (current.Hash != calculatedHash)
            {
                Console.WriteLine($"[DEBUG] Invalid Hash: current.Hash={current.Hash}, calculatedHash={calculatedHash}");
                Console.WriteLine($"[DEBUG] Block data: Index={current.Index}, Nonce={current.Nonce}, Timestamp={current.Timestamp}, PreviousHash={current.PreviousHash}");
                return false;
            }

            string prefix = new string('0', (int)current.Difficulty);
            if (!current.Hash.StartsWith(prefix))
            {
                Console.WriteLine($"[DEBUG] Invalid Hash Prefix: hash={current.Hash}, expected prefix={prefix}");
                return false;
            }

            // TASK 2.3: Validacija timestamp-a
            if (!TimestampValidator.IsValidTimestamp(current, previous))
            {
                Console.WriteLine($"[DEBUG] Invalid Timestamp: current.Timestamp={current.Timestamp}, previous.Timestamp={previous.Timestamp}");
                Console.WriteLine($"[DEBUG] Current time: {DateTimeOffset.UtcNow.ToUnixTimeSeconds()}");
                return false;
            }

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
