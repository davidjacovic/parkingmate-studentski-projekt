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
    /// </summary>
    public class ThreadedMiner
    {
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
            // Dizajn: Podela nonce prostora na jednak broj opsega
            // Thread 0: [0, MAX/N)
            // Thread 1: [MAX/N, 2*MAX/N)
            // Thread 2: [2*MAX/N, 3*MAX/N)
            // ...
            // Thread N-1: [(N-1)*MAX/N, MAX]
            
            if (numThreads <= 0)
                throw new ArgumentException("Broj niti mora biti veći od 0", nameof(numThreads));
            
            if (threadId < 0 || threadId >= numThreads)
                throw new ArgumentException($"Thread ID mora biti između 0 i {numThreads - 1}", nameof(threadId));

            ulong maxNonce = ulong.MaxValue;
            ulong rangeSize = maxNonce / (ulong)numThreads;
            
            ulong startNonce = (ulong)threadId * rangeSize;
            
            // Poslednja nit dobija sve preostale vrednosti do ulong.MaxValue
            ulong endNonce = (threadId == numThreads - 1) 
                ? ulong.MaxValue 
                : ((ulong)(threadId + 1) * rangeSize);

            return (startNonce, endNonce);
        }

        /// <summary>
        /// ThreadPool za paralelno rudarjenje blokova.
        /// Subtask 4.1.2: Implementacija ThreadPool
        /// </summary>
        public class MiningThreadPool
        {
            private readonly int _numThreads;
            private readonly List<Thread> _threads;
            private readonly List<MiningWorker> _workers;

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
            }

            /// <summary>
            /// Broj niti u pool-u
            /// </summary>
            public int ThreadCount => _numThreads;

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
            /// </summary>
            /// <param name="workers">Lista worker objekata - jedan po niti</param>
            public void StartWithWorkers(List<MiningWorker> workers)
            {
                if (workers == null || workers.Count != _numThreads)
                    throw new ArgumentException($"Mora postojati tačno {_numThreads} workers", nameof(workers));

                // Očisti postojeće niti ako postoje
                Stop();

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
            /// Čeka da se sve niti završe sa timeout-om
            /// </summary>
            /// <param name="timeoutMs">Timeout u milisekundama</param>
            /// <returns>True ako su se sve niti završile unutar timeout-a</returns>
            public bool WaitAll(int timeoutMs)
            {
                bool allCompleted = true;
                foreach (var thread in _threads)
                {
                    if (thread.IsAlive)
                    {
                        if (!thread.Join(timeoutMs))
                        {
                            allCompleted = false;
                        }
                    }
                }
                return allCompleted;
            }

            /// <summary>
            /// Zaustavlja sve niti (preko cancellation token-a u workers)
            /// </summary>
            public void Stop()
            {
                // Signaliziraj svim workers da se zaustave
                foreach (var worker in _workers)
                {
                    worker?.Cancel();
                }

                // Čekaj da se niti završe
                WaitAll(5000); // 5 sekundi timeout

                // Ako su neke niti još uvek aktivne, forsiraj ih
                foreach (var thread in _threads)
                {
                    if (thread.IsAlive)
                    {
                        try
                        {
                            thread.Interrupt();
                        }
                        catch
                        {
                            // Ignoriši greške pri interrupt-u
                        }
                    }
                }

                _threads.Clear();
                _workers.Clear();
            }

            /// <summary>
            /// Proverava da li su sve niti završile
            /// </summary>
            public bool AllThreadsCompleted
            {
                get
                {
                    foreach (var thread in _threads)
                    {
                        if (thread.IsAlive)
                            return false;
                    }
                    return true;
                }
            }
        }

        /// <summary>
        /// Worker klasa za rudarjenje u jednoj niti.
        /// Svaki worker dobija svoj opseg nonce vrednosti i traži validan nonce.
        /// </summary>
        public abstract class MiningWorker
        {
            protected volatile bool _cancelled = false;
            protected readonly int _threadId;

            public MiningWorker(int threadId)
            {
                _threadId = threadId;
            }

            /// <summary>
            /// ID niti
            /// </summary>
            public int ThreadId => _threadId;

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
            /// Glavna metoda za izvršavanje rada worker-a - implementira se u nasleđenim klasama
            /// </summary>
            public abstract void Execute();
        }
    }
}


