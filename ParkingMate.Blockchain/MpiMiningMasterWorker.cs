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

        /// <summary>
        /// Struktura za poruku sa pronađenim rešenjem koji worker šalje masteru.
        /// Koristi se za 5.2.3 i 5.2.4.
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

        /// <summary>
        /// Struktura za stop signal koji master šalje workers-ima.
        /// Subtask 5.2.5: Master obaveštava sve čvorove o završetku
        /// </summary>
        public class StopSignalMessage
        {
            public bool ShouldStop { get; set; }
            public string? Reason { get; set; }

            public StopSignalMessage(bool shouldStop, string? reason = null)
            {
                ShouldStop = shouldStop;
                Reason = reason;
            }
        }

        /// <summary>
        /// Master proces koji koordinira distribuirano rudarjenje.
        /// 
        /// Implementirano:
        /// - Subtask 5.2.1: Master generiše seed / nonce opsege
        /// - Subtask 5.2.2: Slanje seed-ova worker procesima
        /// - Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
        /// - Subtask 5.2.5: Master obaveštava sve čvorove o završetku
        /// </summary>
        public class MpiMiningMaster
        {
            private readonly MpiEnvironment _mpi;
            private readonly int _numWorkers;
            private readonly IMpiCommunication _communication;
            private readonly Dictionary<int, MiningResultMessage> _workerResults;

            // Tagovi za MPI komunikaciju
            private const int TAG_NONCE_RANGE = 1; // Tag za slanje nonce opsega workers
            private const int TAG_MINING_RESULT = 2; // Tag za primanje rezultata od workers (5.2.4)
            private const int TAG_STOP_SIGNAL = 3; // Tag za slanje stop signala workers (5.2.5)

            public MpiMiningMaster(MpiEnvironment mpi, IMpiCommunication? communication = null)
            {
                _mpi = mpi ?? throw new ArgumentNullException(nameof(mpi));
                if (!mpi.IsMaster)
                {
                    throw new InvalidOperationException("MpiMiningMaster može biti kreiran samo na master procesu (rank 0)");
                }

                _numWorkers = mpi.Size - 1; // Bez master procesa
                _communication = communication ?? new SimulatedMpiCommunication(mpi);
                _workerResults = new Dictionary<int, MiningResultMessage>();
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

            /// <summary>
            /// Šalje nonce opsege svim worker procesima.
            /// Subtask 5.2.2: Slanje seed-ova worker procesima
            /// Koristi MPI_Send (simulacija ili stvarno) za slanje poruka svakom worker procesu.
            /// </summary>
            /// <param name="messages">Poruke sa nonce opsezima</param>
            public void SendNonceRangesToWorkers(List<NonceRangeMessage> messages)
            {
                if (messages == null || messages.Count == 0)
                {
                    Console.WriteLine($"[Master Rank {_mpi.Rank}] Upozorenje: Nema poruka za slanje workers");
                    return;
                }

                Console.WriteLine($"[Master Rank {_mpi.Rank}] Šaljem {messages.Count} nonce opsega workers...");

                foreach (var message in messages)
                {
                    try
                    {
                        // Koristi IMpiCommunication za slanje poruke
                        // U stvarnom MPI okruženju bi se koristio MPI_Send
                        _communication.Send(message, message.WorkerRank, TAG_NONCE_RANGE);

                        Console.WriteLine($"[Master Rank {_mpi.Rank}] ✓ Poslao nonce opseg [{message.StartNonce:N0}, {message.EndNonce:N0}] worker-u rank {message.WorkerRank}");
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"[Master Rank {_mpi.Rank}] ✗ Greška pri slanju poruke worker-u rank {message.WorkerRank}: {ex.Message}");
                    }
                }

                Console.WriteLine($"[Master Rank {_mpi.Rank}] Svi nonce opsezi su poslati workers");
            }

            /// <summary>
            /// Prima rezultat od workera.
            /// Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
            /// Koristi MPI_Recv (simulacija ili stvarno) za primanje rezultata od workera.
            /// </summary>
            /// <param name="workerRank">Rank workera od koga se prima rezultat</param>
            /// <returns>Primljeni rezultat ili null ako nema poruke</returns>
            public MiningResultMessage? ReceiveResult(int workerRank)
            {
                try
                {
                    // Koristi IMpiCommunication za primanje poruke
                    // U stvarnom MPI okruženju bi se koristio MPI_Recv
                    var result = _communication.Receive<MiningResultMessage>(sourceRank: workerRank, tag: TAG_MINING_RESULT);

                    if (result != null)
                    {
                        _workerResults[result.WorkerRank] = result;
                        Console.WriteLine($"[Master Rank {_mpi.Rank}] ✓ Primio rezultat od worker-a rank {workerRank} (Success: {result.Success})");
                        return result;
                    }
                    else
                    {
                        Console.WriteLine($"[Master Rank {_mpi.Rank}] ⚠ Nema rezultata od worker-a rank {workerRank}");
                        return null;
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Master Rank {_mpi.Rank}] ✗ Greška pri primanju rezultata od worker-a rank {workerRank}: {ex.Message}");
                    return null;
                }
            }

            /// <summary>
            /// Čeka rezultate od svih workers ili prvo pronađeno rešenje.
            /// Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
            /// U simulaciji, proverava sve workers jednom. U stvarnom MPI, ovo bi bilo blocking čekanje.
            /// </summary>
            /// <param name="messages">Poruke poslate workers-ima</param>
            /// <param name="timeoutMs">Timeout u milisekundama</param>
            /// <returns>Prvo pronađeno rešenje ili null ako nije pronađeno</returns>
            public MiningResultMessage? WaitForResults(List<NonceRangeMessage> messages, int timeoutMs = 60000)
            {
                Console.WriteLine($"[Master Rank {_mpi.Rank}] Čekam rezultate od {_numWorkers} workers...");

                // U simulaciji, pokušaj da primiš rezultate od svih workers
                // U stvarnom MPI okruženju bi se koristio MPI_Recv ili MPI_Irecv sa blokiranjem
                foreach (var message in messages)
                {
                    var result = ReceiveResult(message.WorkerRank);
                    
                    // Ako je pronađeno rešenje, vrati ga odmah
                    if (result != null && result.Success && result.FoundBlock != null)
                    {
                        Console.WriteLine($"[Master Rank {_mpi.Rank}] ✓ Pronađeno rešenje od worker-a rank {result.WorkerRank}!");
                        return result;
                    }
                }

                Console.WriteLine($"[Master Rank {_mpi.Rank}] Nema pronađenog rešenja od workers (ili još nisu poslali rezultate)");
                return GetFirstSuccessfulResult();
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

            /// <summary>
            /// Obaveštava sve worker procese da prekinu rad.
            /// Subtask 5.2.5: Master obaveštava sve čvorove o završetku
            /// Koristi MPI_Bcast (simulacija ili stvarno) za slanje stop signala svim workers.
            /// </summary>
            /// <param name="reason">Razlog zaustavljanja (opciono)</param>
            public void NotifyWorkersToStop(string? reason = null)
            {
                var stopSignal = new StopSignalMessage(shouldStop: true, reason: reason);

                try
                {
                    // Koristi IMpiCommunication za broadcast stop signala
                    // U stvarnom MPI okruženju bi se koristio MPI_Bcast
                    _communication.Broadcast(ref stopSignal, rootRank: _mpi.Rank);

                    Console.WriteLine($"[Master Rank {_mpi.Rank}] ✓ Obavešteni su svi workers da prekinu rad");
                    if (!string.IsNullOrEmpty(reason))
                    {
                        Console.WriteLine($"[Master Rank {_mpi.Rank}] Razlog: {reason}");
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Master Rank {_mpi.Rank}] ✗ Greška pri obaveštavanju workers: {ex.Message}");
                }
            }
        }

        /// <summary>
        /// Worker proces koji izvršava rudarjenje na dodeljenom opsegu.
        /// 
        /// Implementirano:
        /// - Subtask 5.2.3: Worker pokreće multi-thread PoW
        /// - Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
        /// - Subtask 5.2.5: Master obaveštava sve čvorove o završetku (primanje stop signala)
        /// </summary>
        public class MpiMiningWorker
        {
            private readonly MpiEnvironment _mpi;
            private readonly int _optimalThreadCount;
            private readonly IMpiCommunication _communication;

            // Tagovi za MPI komunikaciju
            private const int TAG_NONCE_RANGE = 1; // Tag za primanje nonce opsega od mastera
            private const int TAG_MINING_RESULT = 2; // Tag za slanje rezultata masteru (5.2.4)
            private const int TAG_STOP_SIGNAL = 3; // Tag za primanje stop signala od mastera (5.2.5)

            public MpiMiningWorker(MpiEnvironment mpi, IMpiCommunication? communication = null, int? threadCount = null)
            {
                _mpi = mpi ?? throw new ArgumentNullException(nameof(mpi));
                if (mpi.IsMaster)
                {
                    throw new InvalidOperationException("MpiMiningWorker može biti kreiran samo na worker procesima (rank > 0)");
                }

                _optimalThreadCount = threadCount ?? ThreadedMiner.GetOptimalThreadCount();
                _communication = communication ?? new SimulatedMpiCommunication(mpi);
            }

            /// <summary>
            /// Prima nonce opseg od master procesa.
            /// Subtask 5.2.2: Prima seed od master procesa (posledica slanja)
            /// Koristi MPI_Recv (simulacija ili stvarno) za primanje poruke od master procesa.
            /// </summary>
            /// <returns>Poruka sa nonce opsegom ili null ako nema poruke</returns>
            public NonceRangeMessage? ReceiveNonceRange()
            {
                Console.WriteLine($"[Worker Rank {_mpi.Rank}] Čekam nonce opseg od master procesa (rank 0)...");

                try
                {
                    // Koristi IMpiCommunication za primanje poruke
                    // U stvarnom MPI okruženju bi se koristio MPI_Recv
                    var message = _communication.Receive<NonceRangeMessage>(sourceRank: 0, tag: TAG_NONCE_RANGE);

                    if (message != null)
                    {
                        Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✓ Primio nonce opseg [{message.StartNonce:N0}, {message.EndNonce:N0}] od master procesa");
                        return message;
                    }
                    else
                    {
                        Console.WriteLine($"[Worker Rank {_mpi.Rank}] ⚠ Nema poruke od master procesa (možda još nije poslata)");
                        return null;
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✗ Greška pri primanju poruke od master procesa: {ex.Message}");
                    return null;
                }
            }

            /// <summary>
            /// Izvršava multi-threaded rudarjenje na dodeljenom opsegu.
            /// Subtask 5.2.3: Worker pokreće multi-thread PoW
            /// Deli opseg dodeljen ovom worker procesu na lokalne niti i pokreće multi-threaded mining.
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
            /// Prima stop signal od master procesa.
            /// Subtask 5.2.5: Master obaveštava sve čvorove o završetku
            /// Koristi MPI_Bcast (simulacija ili stvarno) za primanje stop signala od master procesa.
            /// </summary>
            /// <returns>Stop signal poruka ili null ako nema signala</returns>
            public StopSignalMessage? ReceiveStopSignal()
            {
                try
                {
                    var stopSignal = new StopSignalMessage(shouldStop: false);
                    
                    // Koristi IMpiCommunication za primanje broadcast poruke
                    // U stvarnom MPI okruženju bi se koristio MPI_Bcast
                    _communication.Broadcast(ref stopSignal, rootRank: 0);

                    if (stopSignal.ShouldStop)
                    {
                        Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✓ Primio stop signal od master procesa");
                        if (!string.IsNullOrEmpty(stopSignal.Reason))
                        {
                            Console.WriteLine($"[Worker Rank {_mpi.Rank}] Razlog: {stopSignal.Reason}");
                        }
                        return stopSignal;
                    }

                    return null;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✗ Greška pri primanju stop signala: {ex.Message}");
                    return null;
                }
            }

            /// <summary>
            /// Šalje rezultat master procesu.
            /// Subtask 5.2.4: Worker vraća pronađeno rešenje masteru
            /// Koristi MPI_Send (simulacija ili stvarno) za slanje rezultata master procesu.
            /// </summary>
            /// <param name="result">Rezultat rudarjenja</param>
            public void SendResultToMaster(MiningResultMessage result)
            {
                try
                {
                    // Koristi IMpiCommunication za slanje rezultata masteru
                    // U stvarnom MPI okruženju bi se koristio MPI_Send
                    _communication.Send(result, destinationRank: 0, tag: TAG_MINING_RESULT);
                    
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✓ Poslao rezultat master procesu (rank 0)");
                    if (result.Success && result.FoundBlock != null)
                    {
                        string hashPreview = result.FoundBlock.Hash != null && result.FoundBlock.Hash.Length > 20
                            ? result.FoundBlock.Hash.Substring(0, 20)
                            : result.FoundBlock.Hash ?? "";
                        Console.WriteLine($"[Worker Rank {_mpi.Rank}] Rezultat: Uspeh - Nonce {result.FoundBlock.Nonce:N0}, Hash: {hashPreview}...");
                    }
                    else
                    {
                        Console.WriteLine($"[Worker Rank {_mpi.Rank}] Rezultat: Nema pronađenog rešenja u opsegu (Pokušaji: {result.TotalAttempts:N0})");
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"[Worker Rank {_mpi.Rank}] ✗ Greška pri slanju rezultata masteru: {ex.Message}");
                }
            }
        }

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

