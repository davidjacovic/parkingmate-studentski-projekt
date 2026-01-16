using System;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Dizajn za paralelno rudarjenje blokova korišćenjem više niti.
    /// Subtask 4.1.1: Dizajn podele nonce prostora po nitima
    /// Subtask 4.1.2: Implementacija ThreadPool
    /// Subtask 4.1.3: Deljeni flag za prekid rada kada se rešenje nađe
    /// Subtask 4.1.4: Sinhronizacija (mutex/atomic) - thread-safe storage i atomic operacije
    /// Subtask 4.2.1: Detekcija broja CPU jezgara
    /// Subtask 5.3.2: Bezbedno gašenje niti
    /// </summary>
    public class ThreadedMiner
    {
        /// <summary>
        /// Detektuje broj CPU jezgara dostupnih na sistemu.
        /// Subtask 4.2.1: Detekcija broja CPU jezgara
        /// </summary>
        /// <returns>Broj logičkih procesora (CPU jezgara) dostupnih na sistemu</returns>
        public static int GetAvailableProcessorCount()
        {
            return Environment.ProcessorCount;
        }

        /// <summary>
        /// Detektuje broj fizičkih CPU jezgara (bez hiperthreading-a).
        /// Pokušava da odredi broj fizičkih jezgara koristeći WMI na Windows-u ili /proc/cpuinfo na Linux-u.
        /// Ako nije moguće odrediti, vraća logički broj procesora podeljen sa 2 (pretpostavka o hiperthreading-u).
        /// Subtask 4.2.1: Detekcija broja CPU jezgara
        /// </summary>
        /// <returns>Broj fizičkih CPU jezgara (aproksimacija)</returns>
        public static int GetPhysicalProcessorCount()
        {
            // Na Windows-u, možemo pokušati da koristimo WMI
            // Na Linux-u, možemo čitati /proc/cpuinfo
            // Za jednostavnost, koristimo Environment.ProcessorCount
            // Većina modernih sistema ima hiperthreading, pa pretpostavljamo da je fizički broj = logički / 2
            // ili ako je neparan, koristimo logički broj
            
            int logicalCores = Environment.ProcessorCount;
            
            // Pokušaj da detektuješ fizičke jezgre kroz sistemske informacije
            // Ovo je aproksimacija - tačan broj zavisi od sistema
            if (logicalCores % 2 == 0 && logicalCores > 2)
            {
                // Verovatno hiperthreading (HT) - fizičkih jezgara je verovatno upola manje
                return logicalCores / 2;
            }
            
            // Ako je neparan ili mali broj, verovatno nema HT ili je broj već fizički
            return logicalCores;
        }

        /// <summary>
        /// Mapira broj CPU jezgara na optimalan broj niti za rudarjenje.
        /// Strategija:
        /// - Za 1-2 jezgra: koristi sve jezgre (1-2 niti)
        /// - Za 3-4 jezgra: koristi sve jezgre (3-4 niti)
        /// - Za 5-8 jezgara: koristi sve jezgre (5-8 niti)
        /// - Za 9+ jezgara: koristi sve jezgre (optimalno je koristiti sve dostupne)
        /// 
        /// Alternativno, možemo koristiti formulu: optimalThreads = logicalCores (za CPU-bound zadatke)
        /// ili: optimalThreads = logicalCores - 1 (da ostavimo jedno jezgro za sistemske zadatke)
        /// 
        /// Subtask 4.2.1: Mapiranje jezgra → broj niti
        /// </summary>
        /// <param name="usePhysicalCores">Ako je true, koristi fizičke jezgre; inače koristi logičke procesore</param>
        /// <param name="reserveCores">Broj jezgara koje treba rezervisati za sistemske zadatke (default: 1)</param>
        /// <returns>Optimalan broj niti za rudarjenje</returns>
        public static int GetOptimalThreadCount(bool usePhysicalCores = false, int reserveCores = 1)
        {
            int availableCores = usePhysicalCores ? GetPhysicalProcessorCount() : GetAvailableProcessorCount();
            
            // Rezerviši jezgra za sistemske zadatke
            int optimalThreads = Math.Max(1, availableCores - reserveCores);
            
            // Osiguraj minimum od 1 niti
            if (optimalThreads < 1)
            {
                optimalThreads = 1;
            }
            
            return optimalThreads;
        }

        /// <summary>
        /// Mapira broj CPU jezgara na optimalan broj niti za rudarjenje (bez rezervisanja jezgara).
        /// Koristi sve dostupne jezgre za maksimalnu performansu.
        /// Subtask 4.2.1: Mapiranje jezgra → broj niti
        /// </summary>
        /// <param name="usePhysicalCores">Ako je true, koristi fizičke jezgre; inače koristi logičke procesore</param>
        /// <returns>Optimalan broj niti za rudarjenje (jednak broju dostupnih jezgara)</returns>
        public static int GetOptimalThreadCountMaxPerformance(bool usePhysicalCores = false)
        {
            return GetOptimalThreadCount(usePhysicalCores, reserveCores: 0);
        }

        /// <summary>
        /// Dizajn podele nonce prostora:
        /// - Nonce prostor (ulong: 0 do 18,446,744,073,709,551,615) se deli na N opsega
        /// - Svaka nit dobija jedinstven opseg: [startNonce, endNonce)
        /// - Opseg se računa kao: threadId * (MAX_NONCE / numThreads) do (threadId + 1) * (MAX_NONCE / numThreads)
        /// </summary>
        /// <param name="numThreads">Broj niti</param>
        /// <param name="threadId">ID trenutne niti (0-based)</param>
        /// <returns>Par (startNonce, endNonce) za datu nit</returns>
        public static (ulong startNonce, ulong endNonce) CalculateNonceRange(int numThreads, int threadId)
        {
            if (numThreads <= 0)
                throw new ArgumentException("Broj niti mora biti veći od 0", nameof(numThreads));

            if (threadId < 0 || threadId >= numThreads)
                throw new ArgumentException($"Thread ID mora biti između 0 i {numThreads - 1}", nameof(threadId));

            ulong maxNonce = ulong.MaxValue;
            ulong rangeSize = maxNonce / (ulong)numThreads;

            ulong startNonce = (ulong)threadId * rangeSize;

            // INCLUSIVE endNonce:
            ulong endNonce = (threadId == numThreads - 1)
                ? ulong.MaxValue
                : (((ulong)(threadId + 1) * rangeSize) - 1);

            return (startNonce, endNonce);
        }

        /// <summary>
        /// Thread-safe shared state za koordinaciju rudarjenja između niti.
        /// Subtask 4.1.3: Deljeni flag za prekid rada kada se rešenje nađe
        /// Subtask 4.1.4: Sinhronizacija (mutex/atomic) za thread-safe storage rezultata
        /// </summary>
        public class SharedMiningState
        {
            // Thread-safe flag za signalizaciju da je rešenje pronađeno
            // Koristimo Interlocked.CompareExchange za atomic operacije
            private int _solutionFound = 0; // 0 = nije pronađeno, 1 = pronađeno

            // Thread-safe storage za pronađeni blok
            // Koristimo object lock za zaštitu pristupa
            private readonly object _resultLock = new object();
            private Block? _foundBlock = null;
            private int _foundByThreadId = -1;

            // Thread-safe counter za statistiku (broj pokušaja)
            private long _totalAttempts = 0;

            /// <summary>
            /// Proverava da li je rešenje već pronađeno
            /// </summary>
            public bool IsSolutionFound()
            {
                return Thread.VolatileRead(ref _solutionFound) == 1;
            }

            /// <summary>
            /// Postavlja flag da je rešenje pronađeno.
            /// Vraća true ako je ovaj thread prvi postavio flag (pronašao rešenje),
            /// false ako je neki drugi thread već postavio flag.
            /// </summary>
            public bool TrySetSolutionFound()
            {
                // Atomic operacija: postavi na 1 samo ako je trenutna vrednost 0
                int originalValue = Interlocked.CompareExchange(ref _solutionFound, 1, 0);
                return originalValue == 0; // True ako je ovaj thread postavio flag
            }

            /// <summary>
            /// Thread-safe postavljanje pronađenog bloka.
            /// Vraća true ako je blok uspešno postavljen (prvi thread koji je našao rešenje),
            /// false ako je neki drugi thread već postavio blok.
            /// Subtask 4.1.4: Sinhronizacija pomoću lock-a
            /// </summary>
            public bool TrySetFoundBlock(Block block, int threadId)
            {
                // Prvo proveri da li je već pronađeno (fast path)
                if (IsSolutionFound())
                {
                    return false;
                }

                // Atomically postavi flag i istovremeno proveri da li je uspeo
                bool isFirst = TrySetSolutionFound();
                
                if (isFirst)
                {
                    // Ovo je prvi thread koji je pronašao rešenje
                    // Koristimo lock za thread-safe postavljanje bloka
                    lock (_resultLock)
                    {
                        // Ponovna provera (double-check locking pattern)
                        if (_foundBlock == null)
                        {
                            _foundBlock = block;
                            _foundByThreadId = threadId;
                        }
                    }
                }

                return isFirst;
            }

            /// <summary>
            /// Thread-safe čitanje pronađenog bloka.
            /// Vraća pronađeni blok ili null ako nije pronađen.
            /// Subtask 4.1.4: Sinhronizacija pomoću lock-a
            /// </summary>
            public Block? GetFoundBlock()
            {
                lock (_resultLock)
                {
                    return _foundBlock;
                }
            }

            /// <summary>
            /// Vraća ID niti koja je pronašla rešenje.
            /// Subtask 4.1.4: Thread-safe čitanje
            /// </summary>
            public int GetFoundByThreadId()
            {
                lock (_resultLock)
                {
                    return _foundByThreadId;
                }
            }

            /// <summary>
            /// Thread-safe inkrementiranje brojača pokušaja.
            /// Koristi Interlocked za atomic operaciju.
            /// Subtask 4.1.4: Atomic operacija
            /// </summary>
            public void IncrementAttempts()
            {
                Interlocked.Increment(ref _totalAttempts);
            }

            /// <summary>
            /// Thread-safe čitanje ukupnog broja pokušaja.
            /// Subtask 4.1.4: Thread-safe čitanje
            /// </summary>
            public long GetTotalAttempts()
            {
                return Interlocked.Read(ref _totalAttempts);
            }

            /// <summary>
            /// Resetuje flag i sve podatke (korisno za ponovno pokretanje rudarjenja)
            /// Subtask 4.1.4: Thread-safe reset sa lock-om
            /// </summary>
            public void Reset()
            {
                Interlocked.Exchange(ref _solutionFound, 0);
                
                lock (_resultLock)
                {
                    _foundBlock = null;
                    _foundByThreadId = -1;
                }
                
                Interlocked.Exchange(ref _totalAttempts, 0);
            }

            /// <summary>
            /// Thread-safe provera i postavljanje - koristi se za optimizaciju
            /// </summary>
            public bool CheckAndStopIfFound()
            {
                return IsSolutionFound();
            }
        }

        /// <summary>
        /// ThreadPool za paralelno rudarjenje blokova.
        /// Subtask 4.1.2: Implementacija ThreadPool
        /// Subtask 4.1.3: Integracija sa deljenim flag-om
        /// </summary>
        public class MiningThreadPool
        {
            private readonly int _numThreads;
            private readonly List<Thread> _threads;
            private readonly List<MiningWorker> _workers;
            private SharedMiningState _sharedState;

            /// <summary>
            /// Kreira novi ThreadPool sa određenim brojem niti
            /// </summary>
            /// <param name="numThreads">Broj niti u pool-u</param>
            public MiningThreadPool(int numThreads)
            {
                if (numThreads <= 0)
                    throw new ArgumentException("Broj niti mora biti veći od 0", nameof(numThreads));

                _numThreads = numThreads;
                _threads = new List<Thread>(numThreads);
                _workers = new List<MiningWorker>(numThreads);
                _sharedState = new SharedMiningState();
            }

            /// <summary>
            /// Broj niti u pool-u
            /// </summary>
            public int ThreadCount => _numThreads;

            /// <summary>
            /// Deljeni state za koordinaciju niti
            /// Subtask 4.1.3: Pristup deljenom flag-u
            /// </summary>
            public SharedMiningState SharedState => _sharedState;

            /// <summary>
            /// Kreira i pokreće sve niti u pool-u sa određenim radnim zadatkom
            /// </summary>
            /// <param name="workAction">Akcija koja se izvršava u svakoj niti</param>
            public void Start(Action<int> workAction)
            {
                if (workAction == null)
                    throw new ArgumentNullException(nameof(workAction));

                // Očisti postojeće niti ako postoje
                Stop();

                // Resetuj shared state za novi ciklus rudarjenja
                _sharedState.Reset();

                // Kreiraj i pokreni nove niti
                for (int i = 0; i < _numThreads; i++)
                {
                    int threadId = i; // Capture za closure
                    var thread = new Thread(() => workAction(threadId))
                    {
                        IsBackground = false,
                        Name = $"MiningThread-{threadId}"
                    };
                    _threads.Add(thread);
                    thread.Start();
                }
            }

            /// <summary>
            /// Kreira i pokreće sve niti sa worker objektima
            /// Subtask 4.1.3: Workers moraju imati isti SharedMiningState
            /// </summary>
            /// <param name="workers">Lista worker objekata - jedan po niti</param>
            public void StartWithWorkers(List<MiningWorker> workers)
            {
                if (workers == null || workers.Count != _numThreads)
                    throw new ArgumentException($"Mora postojati tačno {_numThreads} workers", nameof(workers));

                // Očisti postojeće niti ako postoje
                Stop();

                // Resetuj shared state za novi ciklus rudarjenja
                _sharedState.Reset();

                // Proveri da svi workers dele isti shared state
                foreach (var worker in workers)
                {
                    if (worker.SharedState != _sharedState)
                    {
                        throw new ArgumentException("Svi workers moraju imati isti SharedMiningState", nameof(workers));
                    }
                }

                _workers.Clear();
                _workers.AddRange(workers);

                // Kreiraj i pokreni niti za svaki worker
                for (int i = 0; i < _numThreads; i++)
                {
                    int threadId = i; // Capture za closure
                    var worker = workers[i];
                    var thread = new Thread(() => worker.Execute())
                    {
                        IsBackground = false,
                        Name = $"MiningThread-{threadId}"
                    };
                    _threads.Add(thread);
                    thread.Start();
                }
            }

            /// <summary>
            /// Čeka da se sve niti završe
            /// </summary>
            public void WaitAll()
            {
                foreach (var thread in _threads)
                {
                    if (thread.IsAlive)
                    {
                        thread.Join();
                    }
                }
            }

            /// <summary>
            /// Čeka da se sve niti završe sa timeout-om.
            /// Subtask 5.3.2: Bezbedno gašenje niti - bezbedno čekanje završetka
            /// </summary>
            /// <param name="timeoutMs">Timeout u milisekundama</param>
            /// <returns>True ako su se sve niti završile unutar timeout-a</returns>
            public bool WaitAll(int timeoutMs)
            {
                if (_threads.Count == 0)
                {
                    return true; // Nema niti za čekanje
                }

                bool allCompleted = true;
                int remainingTimeout = timeoutMs;
                int threadCount = _threads.Count;
                
                // Podeli timeout jednakomerno između svih niti
                int timeoutPerThread = timeoutMs / threadCount;
                if (timeoutPerThread < 100)
                {
                    timeoutPerThread = 100; // Minimum 100ms po niti
                }

                foreach (var thread in _threads)
                {
                    if (thread.IsAlive)
                    {
                        // Koristi thread.Join sa timeout-om za bezbedno čekanje
                        bool joined = thread.Join(timeoutPerThread);
                        if (!joined)
                        {
                            allCompleted = false;
                            // Ako nit nije završila, proveri da li još radi
                            if (thread.IsAlive)
                            {
                                // Nit još radi - oduzmi korišćeno vreme od preostalog timeout-a
                                remainingTimeout -= timeoutPerThread;
                                if (remainingTimeout <= 0)
                                {
                                    break; // Nema više vremena
                                }
                            }
                        }
                    }
                }
                
                return allCompleted;
            }

            /// <summary>
            /// Zaustavlja sve niti (preko cancellation token-a u workers i shared flag-a)
            /// Subtask 4.1.3: Koristi shared flag za signalizaciju
            /// Subtask 5.3.2: Bezbedno gašenje niti
            /// </summary>
            public void Stop()
            {
                if (_threads.Count == 0)
                {
                    return; // Već zaustavljeno ili nije pokrenuto
                }

                Console.WriteLine($"[ThreadPool] Zaustavljam {_threads.Count} niti...");

                // Korak 1: Postavi shared flag da signalizira zaustavljanje
                _sharedState.TrySetSolutionFound();

                // Korak 2: Signaliziraj svim workers da se zaustave (graceful shutdown)
                foreach (var worker in _workers)
                {
                    worker?.Cancel();
                }

                // Korak 3: Bezbedno čekaj da se niti završe (graceful shutdown)
                bool allStopped = WaitAll(5000); // 5 sekundi timeout za graceful shutdown

                if (allStopped)
                {
                    Console.WriteLine($"[ThreadPool] ✓ Sve niti su bezbedno zaustavljene");
                }
                else
                {
                    Console.WriteLine($"[ThreadPool] ⚠ Neke niti nisu završile u roku od 5 sekundi");

                    // Korak 4: Ako su neke niti još uvek aktivne, pokušaj interrupt (forceful shutdown)
                    foreach (var thread in _threads)
                    {
                        if (thread.IsAlive)
                        {
                            try
                            {
                                thread.Interrupt(); // Pokušaj interrupt ako nit čeka
                                Console.WriteLine($"[ThreadPool] Pokušavam interrupt thread-a {thread.ManagedThreadId}");
                            }
                            catch (Exception ex)
                            {
                                Console.WriteLine($"[ThreadPool] Greška pri interrupt-u thread-a {thread.ManagedThreadId}: {ex.Message}");
                            }
                        }
                    }

                    // Korak 5: Sačekaj još malo nakon interrupt-a
                    Thread.Sleep(500);
                    
                    // Proveri ponovo
                    int aliveThreads = 0;
                    foreach (var thread in _threads)
                    {
                        if (thread.IsAlive)
                        {
                            aliveThreads++;
                            Console.WriteLine($"[ThreadPool] ⚠ Thread {thread.ManagedThreadId} još uvek radi");
                        }
                    }

                    if (aliveThreads == 0)
                    {
                        Console.WriteLine($"[ThreadPool] ✓ Sve niti su sada zaustavljene");
                    }
                    else
                    {
                        Console.WriteLine($"[ThreadPool] ⚠ Upozorenje: {aliveThreads} niti još uvek radi (možda blokirane)");
                    }
                }

                // Korak 6: Očisti resurse
                _threads.Clear();
                _workers.Clear();
                
                Console.WriteLine($"[ThreadPool] Cleanup završen");
            }

            /// <summary>
            /// Proverava da li su sve niti završile.
            /// Subtask 5.3.2: Bezbedno gašenje niti - provera statusa
            /// </summary>
            public bool AllThreadsCompleted
            {
                get
                {
                    if (_threads.Count == 0)
                    {
                        return true; // Nema niti
                    }

                    foreach (var thread in _threads)
                    {
                        if (thread.IsAlive)
                            return false;
                    }
                    return true;
                }
            }

            /// <summary>
            /// Vraća broj aktivnih niti.
            /// Subtask 5.3.2: Bezbedno gašenje niti - monitoring statusa
            /// </summary>
            public int ActiveThreadCount
            {
                get
                {
                    int count = 0;
                    foreach (var thread in _threads)
                    {
                        if (thread.IsAlive)
                            count++;
                    }
                    return count;
                }
            }

            /// <summary>
            /// Proverava da li ThreadPool ima pokrenute niti.
            /// Subtask 5.3.2: Bezbedno gašenje niti - provera da li je aktivno
            /// </summary>
            public bool IsRunning => _threads.Count > 0 && !AllThreadsCompleted;
        }

        /// <summary>
        /// Worker klasa za rudarjenje u jednoj niti.
        /// Svaki worker dobija svoj opseg nonce vrednosti i traži validan nonce.
        /// Subtask 4.1.3: Integracija sa deljenim flag-om za prekid rada
        /// </summary>
        public abstract class MiningWorker
        {
            protected volatile bool _cancelled = false;
            protected readonly int _threadId;
            protected readonly SharedMiningState _sharedState;

            /// <summary>
            /// Kreira novi MiningWorker
            /// </summary>
            /// <param name="threadId">ID niti</param>
            /// <param name="sharedState">Deljeni state za koordinaciju sa drugim nitima</param>
            public MiningWorker(int threadId, SharedMiningState sharedState)
            {
                _threadId = threadId;
                _sharedState = sharedState ?? throw new ArgumentNullException(nameof(sharedState));
            }

            /// <summary>
            /// ID niti
            /// </summary>
            public int ThreadId => _threadId;

            /// <summary>
            /// Deljeni state za koordinaciju
            /// </summary>
            public SharedMiningState SharedState => _sharedState;

            /// <summary>
            /// Signalizira worker-u da se zaustavi
            /// </summary>
            public virtual void Cancel()
            {
                _cancelled = true;
            }

            /// <summary>
            /// Proverava da li je worker otkazan
            /// </summary>
            public bool IsCancelled => _cancelled;

            /// <summary>
            /// Proverava da li je rešenje pronađeno (kroz deljeni flag ili direktno otkazan)
            /// </summary>
            protected bool ShouldStop()
            {
                return _cancelled || _sharedState.IsSolutionFound();
            }

            /// <summary>
            /// Pokušava da postavi flag da je rešenje pronađeno.
            /// Vraća true ako je ovaj worker prvi pronašao rešenje.
            /// </summary>
            protected bool TryMarkSolutionFound()
            {
                return _sharedState.TrySetSolutionFound();
            }

            /// <summary>
            /// Glavna metoda za izvršavanje rada worker-a - implementira se u nasleđenim klasama
            /// </summary>
            public abstract void Execute();
        }

        /// <summary>
        /// Konkretna implementacija MiningWorker-a za rudarjenje blokova.
        /// Traži validan nonce u dodeljenom opsegu.
        /// </summary>
        public class BlockMiningWorker : MiningWorker
        {
            private readonly Block _blockToMine;
            private readonly ulong _startNonce;
            private readonly ulong _endNonce;
            private readonly string _targetPrefix;

            /// <summary>
            /// Kreira novi BlockMiningWorker
            /// </summary>
            /// <param name="threadId">ID niti</param>
            /// <param name="sharedState">Deljeni state</param>
            /// <param name="blockToMine">Blok koji se rudari</param>
            /// <param name="startNonce">Početni nonce za ovu nit</param>
            /// <param name="endNonce">Završni nonce za ovu nit</param>
            public BlockMiningWorker(
                int threadId,
                SharedMiningState sharedState,
                Block blockToMine,
                ulong startNonce,
                ulong endNonce) : base(threadId, sharedState)
            {
                _blockToMine = blockToMine ?? throw new ArgumentNullException(nameof(blockToMine));
                _startNonce = startNonce;
                _endNonce = endNonce;
                _targetPrefix = new string('0', (int)blockToMine.Difficulty);
            }

            /// <summary>
            /// Izvršava rudarjenje - traži validan nonce u dodeljenom opsegu
            /// </summary>
            public override void Execute()
            {
                ulong currentNonce = _startNonce;

                // Rudari dok ne nađeš validan nonce ili dok ne završiš opseg ili dok ne bude signalizirano da se zaustavi
                while (currentNonce <= _endNonce && !ShouldStop())
                {
                    _blockToMine.Nonce = currentNonce;
                    string hash = _blockToMine.CalculateHash();
                    _sharedState.IncrementAttempts();

                    if (hash.StartsWith(_targetPrefix))
                    {
                        _blockToMine.Hash = hash;
                        _sharedState.TrySetFoundBlock(_blockToMine, _threadId);
                        return;
                    }

                    if (currentNonce == ulong.MaxValue)
                        break;

                    currentNonce++;
                }
            }
        }
    }
}


