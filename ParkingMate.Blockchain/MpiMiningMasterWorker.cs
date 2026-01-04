using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// MPI Master-Worker arhitektura za distribuirano rudarjenje blokova.
    /// 
    /// Implementirano:
    /// - Subtask 5.2.1: Master generiše seed / nonce opsege
    /// 
    /// TODO (naredni subtaskovi):
    /// - Subtask 5.2.2: Slanje seed-ova worker procesima
    /// - Subtask 5.2.3: Worker pokreće multi-thread PoW
    /// - Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
    /// - Subtask 5.2.5: Master obaveštava sve čvorove o završetku
    /// </summary>
    public class MpiMiningMasterWorker
    {
        /// <summary>
        /// Struktura za poruku sa nonce opsegom koji master šalje worker-u
        /// </summary>
        public class NonceRangeMessage
        {
            public ulong StartNonce { get; set; }
            public ulong EndNonce { get; set; }
            public int WorkerRank { get; set; }
            public Block BlockToMine { get; set; }

            public NonceRangeMessage(ulong startNonce, ulong endNonce, int workerRank, Block blockToMine)
            {
                StartNonce = startNonce;
                EndNonce = endNonce;
                WorkerRank = workerRank;
                BlockToMine = blockToMine;
            }
        }

        // TODO: 5.2.4 - Struktura za poruku sa pronađenim rešenjem koji worker šalje masteru
        /*
        /// <summary>
        /// Struktura za poruku sa pronađenim rešenjem koji worker šalje masteru
        /// </summary>
        public class MiningResultMessage
        {
            public Block? FoundBlock { get; set; }
            public int WorkerRank { get; set; }
            public long MiningTimeMs { get; set; }
            public long TotalAttempts { get; set; }
            public bool Success { get; set; }

            public MiningResultMessage(int workerRank)
            {
                WorkerRank = workerRank;
                Success = false;
            }
        }
        */

        /// <summary>
        /// Master proces koji koordinira distribuirano rudarjenje.
        /// 
        /// Implementirano:
        /// - Subtask 5.2.1: Master generiše seed / nonce opsege
        /// 
        /// TODO:
        /// - Subtask 5.2.2: Slanje seed-ova worker procesima
        /// - Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
        /// - Subtask 5.2.5: Master obaveštava sve čvorove o završetku
        /// </summary>
        public class MpiMiningMaster
        {
            private readonly MpiEnvironment _mpi;
            private readonly int _numWorkers;
            // TODO: 5.2.4 - Dictionary za rezultate od workers
            // private readonly Dictionary<int, MiningResultMessage> _workerResults;

            public MpiMiningMaster(MpiEnvironment mpi)
            {
                _mpi = mpi ?? throw new ArgumentNullException(nameof(mpi));
                if (!mpi.IsMaster)
                {
                    throw new InvalidOperationException("MpiMiningMaster može biti kreiran samo na master procesu (rank 0)");
                }

                _numWorkers = mpi.Size - 1; // Bez master procesa
                // TODO: 5.2.4 - Inicijalizacija dictionary-a za rezultate
                // _workerResults = new Dictionary<int, MiningResultMessage>();
            }

            /// <summary>
            /// Generiše nonce opsege za sve worker procese.
            /// Subtask 5.2.1: Master generiše seed / nonce opsege
            /// </summary>
            /// <param name="blockToMine">Blok koji se rudari</param>
            /// <returns>Lista poruka sa nonce opsezima za svaki worker</returns>
            public List<NonceRangeMessage> GenerateNonceRanges(Block blockToMine)
            {
                var messages = new List<NonceRangeMessage>();

                // Podeli nonce prostor na worker procese
                // Koristi istu logiku kao za threads, ali sada na nivou procesa
                for (int i = 0; i < _numWorkers; i++)
                {
                    int workerRank = i + 1; // Worker ranks su 1, 2, 3, ..., Size-1

                    var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(_numWorkers, i);

                    // Napravi kopiju bloka za svakog workera
                    var workerBlock = new Block(
                        blockToMine.Index,
                        blockToMine.Data,
                        blockToMine.Timestamp,
                        blockToMine.PreviousHash,
                        blockToMine.Difficulty,
                        blockToMine.Nonce
                    );

                    var message = new NonceRangeMessage(startNonce, endNonce, workerRank, workerBlock);
                    messages.Add(message);
                }

                return messages;
            }

            // TODO: 5.2.2 - Slanje seed-ova worker procesima
            /*
            /// <summary>
            /// Šalje nonce opsege svim worker procesima.
            /// Subtask 5.2.2: Slanje seed-ova worker procesima
            /// </summary>
            /// <param name="messages">Poruke sa nonce opsezima</param>
            public void SendNonceRangesToWorkers(List<NonceRangeMessage> messages)
            {
                foreach (var message in messages)
                {
                    // Simulacija MPI_Send - u stvarnom MPI okruženju bi se koristio MPI_Send
                    Console.WriteLine($"[Master Rank {_mpi.Rank}] Šaljem nonce opseg [{message.StartNonce:N0}, {message.EndNonce:N0}] worker-u rank {message.WorkerRank}");
                }
            }
            */

            // TODO: 5.2.4, 5.2.5 - Čekanje rezultata od workers
            /*
            /// <summary>
            /// Čeka rezultate od svih workers ili prvo pronađeno rešenje.
            /// Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
            /// </summary>
            /// <param name="messages">Poruke poslate workers-ima</param>
            /// <param name="timeoutMs">Timeout u milisekundama</param>
            /// <returns>Prvo pronađeno rešenje ili null ako nije pronađeno</returns>
            public MiningResultMessage? WaitForResults(List<NonceRangeMessage> messages, int timeoutMs = 60000)
            {
                // Simulacija MPI_Recv - u stvarnom MPI okruženju bi se koristio MPI_Recv
                // Za sada, ovo će biti simulirano kroz shared state ili callback mehanizam
                // U stvarnoj implementaciji, ovo bi blokiralo dok ne dobije poruku od nekog workera

                Console.WriteLine($"[Master Rank {_mpi.Rank}] Čekam rezultate od {_numWorkers} workers...");

                // Ovo je simulacija - u stvarnom MPI okruženju bi čekao MPI poruke
                // Za testiranje, koristićemo in-memory komunikaciju
                return null; // Vraća se iz stvarne implementacije
            }

            /// <summary>
            /// Obaveštava sve worker procese da prekinu rad.
            /// Subtask 5.2.5: Master obaveštava sve čvorove o završetku
            /// </summary>
            public void NotifyWorkersToStop()
            {
                // Simulacija MPI_Bcast - u stvarnom MPI okruženju bi se koristio MPI_Bcast
                Console.WriteLine($"[Master Rank {_mpi.Rank}] Obaveštavam sve workers da prekinu rad...");
            }

            /// <summary>
            /// Prima rezultat od workera.
            /// Subtask 5.2.4
            /// </summary>
            public void ReceiveResult(MiningResultMessage result)
            {
                _workerResults[result.WorkerRank] = result;
            }

            /// <summary>
            /// Proverava da li ima pronađeno rešenje od bilo kog workera
            /// Subtask 5.2.4
            /// </summary>
            public MiningResultMessage? GetFirstSuccessfulResult()
            {
                foreach (var result in _workerResults.Values)
                {
                    if (result.Success && result.FoundBlock != null)
                    {
                        return result;
                    }
                }
                return null;
            }
            */
        }

        // TODO: 5.2.3, 5.2.4 - Worker proces koji izvršava rudarjenje
        /*
        /// <summary>
        /// Worker proces koji izvršava rudarjenje na dodeljenom opsegu.
        /// Subtask 5.2.3: Worker pokreće multi-thread PoW
        /// Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
        /// </summary>
        public class MpiMiningWorker
        {
            private readonly MpiEnvironment _mpi;
            private readonly int _optimalThreadCount;

            public MpiMiningWorker(MpiEnvironment mpi, int? threadCount = null)
            {
                _mpi = mpi ?? throw new ArgumentNullException(nameof(mpi));
                if (mpi.IsMaster)
                {
                    throw new InvalidOperationException("MpiMiningWorker može biti kreiran samo na worker procesima (rank > 0)");
                }

                _optimalThreadCount = threadCount ?? ThreadedMiner.GetOptimalThreadCount();
            }

            /// <summary>
            /// Prima nonce opseg od master procesa.
            /// Subtask 5.2.1: Slanje seed-ova worker procesima
            /// </summary>
            /// <returns>Poruka sa nonce opsegom</returns>
            public NonceRangeMessage? ReceiveNonceRange()
            {
                // Simulacija MPI_Recv - u stvarnom MPI okruženju bi se koristio MPI_Recv
                Console.WriteLine($"[Worker Rank {_mpi.Rank}] Čekam nonce opseg od master procesa...");
                return null; // Vraća se iz stvarne implementacije
            }

            /// <summary>
            /// Izvršava multi-threaded rudarjenje na dodeljenom opsegu.
            /// Subtask 5.2.1: Worker pokreće multi-thread PoW
            /// </summary>
            /// <param name="message">Poruka sa nonce opsegom</param>
            /// <returns>Rezultat rudarjenja</returns>
            public MiningResultMessage ExecuteMining(NonceRangeMessage message)
            {
                Console.WriteLine($"[Worker Rank {_mpi.Rank}] Pokrećem rudarjenje sa {_optimalThreadCount} niti...");
                Console.WriteLine($"[Worker Rank {_mpi.Rank}] Nonce opseg: [{message.StartNonce:N0}, {message.EndNonce:N0}]");

                var result = new MiningResultMessage(_mpi.Rank);
                var stopwatch = System.Diagnostics.Stopwatch.StartNew();

                // Kreiraj ThreadPool sa optimalnim brojem niti
                var pool = new ThreadedMiner.MiningThreadPool(_optimalThreadCount);
                var sharedState = pool.SharedState;

                // Kreiraj workers za svaku lokalnu nit
                var workers = new List<ThreadedMiner.MiningWorker>();
                
                // Podeli opseg dodeljen ovom worker procesu na lokalne niti
                ulong workerRangeSize = message.EndNonce - message.StartNonce + 1;
                ulong threadRangeSize = workerRangeSize / (ulong)_optimalThreadCount;
                
                for (int i = 0; i < _optimalThreadCount; i++)
                {
                    ulong threadStartNonce = message.StartNonce + (ulong)i * threadRangeSize;
                    ulong threadEndNonce = (i == _optimalThreadCount - 1) 
                        ? message.EndNonce 
                        : message.StartNonce + ((ulong)(i + 1) * threadRangeSize) - 1;

                    var blockCopy = new Block(
                        message.BlockToMine.Index,
                        message.BlockToMine.Data,
                        message.BlockToMine.Timestamp,
                        message.BlockToMine.PreviousHash,
                        message.BlockToMine.Difficulty,
                        message.BlockToMine.Nonce
                    );

                    var worker = new ThreadedMiner.BlockMiningWorker(
                        threadId: i,
                        sharedState: sharedState,
                        blockToMine: blockCopy,
                        startNonce: threadStartNonce,
                        endNonce: threadEndNonce
                    );

                    workers.Add(worker);
                }

                // Pokreni multi-threaded mining
                pool.StartWithWorkers(workers);
                pool.WaitAll();
                stopwatch.Stop();

                // Prikupi rezultate
                result.MiningTimeMs = stopwatch.ElapsedMilliseconds;
                result.TotalAttempts = sharedState.GetTotalAttempts();
                result.FoundBlock = sharedState.GetFoundBlock();

                if (result.FoundBlock != null)
                {
                    result.Success = true;
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✓ Rešenje pronađeno! Nonce: {result.FoundBlock.Nonce:N0}");
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] Vreme: {result.MiningTimeMs} ms, Pokušaji: {result.TotalAttempts:N0}");
                }
                else
                {
                    result.Success = false;
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✗ Rešenje nije pronađeno u dodeljenom opsegu");
                }

                pool.Stop();
                return result;
            }

            /// <summary>
            /// Šalje rezultat master procesu.
            /// Subtask 5.2.1: Worker vraća pronađeno rešenje masteru
            /// </summary>
            /// <param name="result">Rezultat rudarjenja</param>
            public void SendResultToMaster(MiningResultMessage result)
            {
                // Simulacija MPI_Send - u stvarnom MPI okruženju bi se koristio MPI_Send
                Console.WriteLine($"[Worker Rank {_mpi.Rank}] Šaljem rezultat master procesu (rank 0)...");
                if (result.Success)
                {
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] Rezultat: Uspeh - Nonce {result.FoundBlock?.Nonce:N0}");
                }
            }
        }
        */

        // TODO: Integracija Master-Worker arhitekture (nakon 5.2.5)
        /*
        /// <summary>
        /// Integrisana klasa koja pokreće master ili worker logiku na osnovu MPI rank-a.
        /// </summary>
        public class MpiMiningOrchestrator
        {
            private readonly MpiEnvironment _mpi;
            private readonly int? _threadCountPerWorker;

            public MpiMiningOrchestrator(MpiEnvironment mpi, int? threadCountPerWorker = null)
            {
                _mpi = mpi ?? throw new ArgumentNullException(nameof(mpi));
                if (!mpi.IsInitialized)
                {
                    throw new InvalidOperationException("MPI okruženje mora biti inicijalizovano");
                }

                _threadCountPerWorker = threadCountPerWorker;
            }

            /// <summary>
            /// Pokreće master logiku - generiše opsege i koordinira workers
            /// </summary>
            /// <param name="blockToMine">Blok koji se rudari</param>
            /// <returns>Pronađeni blok ili null</returns>
            public Block? RunMaster(Block blockToMine)
            {
                if (!_mpi.IsMaster)
                {
                    throw new InvalidOperationException("RunMaster može biti pozvano samo na master procesu");
                }

                var master = new MpiMiningMaster(_mpi);
                
                // Generiši nonce opsege za sve workers
                var messages = master.GenerateNonceRanges(blockToMine);
                Console.WriteLine($"[Master Rank {_mpi.Rank}] Generisano {messages.Count} nonce opsega za workers");

                // Pošalji opsege workers
                master.SendNonceRangesToWorkers(messages);

                // U stvarnom MPI okruženju, ovde bi master čekao rezultate od workers
                // Za simulaciju, koristićemo in-memory komunikaciju
                Console.WriteLine($"[Master Rank {_mpi.Rank}] Čekam rezultate od workers...");

                // Simulacija - u stvarnom MPI bi koristili MPI_Recv ili MPI_Irecv
                return null;
            }

            /// <summary>
            /// Pokreće worker logiku - prima opseg i izvršava rudarjenje
            /// </summary>
            /// <param name="message">Poruka sa nonce opsegom</param>
            /// <returns>Rezultat rudarjenja</returns>
            public MiningResultMessage RunWorker(NonceRangeMessage message)
            {
                if (_mpi.IsMaster)
                {
                    throw new InvalidOperationException("RunWorker može biti pozvano samo na worker procesima");
                }

                var worker = new MpiMiningWorker(_mpi, _threadCountPerWorker);
                
                // Izvrši multi-threaded mining
                var result = worker.ExecuteMining(message);
                
                // Pošalji rezultat masteru
                worker.SendResultToMaster(result);
                
                return result;
            }
        }
        */
    }
}

