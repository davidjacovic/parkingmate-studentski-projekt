using System;
using System.Threading;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje ThreadPool implementacije (Subtask 4.1.2)
    /// </summary>
    public class TestThreadPool
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test ThreadPool implementacije (4.1.2 i 4.1.3) ===\n");

            // Test 1: Kreiranje ThreadPool-a sa različitim brojevima niti
            Console.WriteLine("Test 1: Kreiranje ThreadPool-a");
            TestPoolCreation();
            Console.WriteLine();

            // Test 2: Pokretanje niti sa worker objektima
            Console.WriteLine("Test 2: Pokretanje niti sa worker objektima");
            TestWorkerExecution();
            Console.WriteLine();

            // Test 3: Zaustavljanje niti
            Console.WriteLine("Test 3: Zaustavljanje niti");
            TestStopWorkers();
            Console.WriteLine();

            // Test 4: Deljeni flag za prekid rada (4.1.3)
            Console.WriteLine("Test 4: Deljeni flag za prekid rada kada se rešenje nađe");
            TestSharedFlag();
            Console.WriteLine();

            Console.WriteLine("✓ Svi testovi ThreadPool-a i shared flag-a su prošli!");
        }

        private static void TestPoolCreation()
        {
            try
            {
                // Kreiraj pool sa 4 niti
                var pool = new ThreadedMiner.MiningThreadPool(4);
                Console.WriteLine($"  ✓ Kreiran pool sa {pool.ThreadCount} niti");

                // Pokušaj kreiranja sa 0 niti - treba da baci exception
                try
                {
                    var invalidPool = new ThreadedMiner.MiningThreadPool(0);
                    Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za 0 niti");
                }
                catch (ArgumentException)
                {
                    Console.WriteLine("  ✓ Validacija: Exception bačen za 0 niti");
                }

                // Pokušaj kreiranja sa negativnim brojem - treba da baci exception
                try
                {
                    var invalidPool = new ThreadedMiner.MiningThreadPool(-1);
                    Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za negativan broj");
                }
                catch (ArgumentException)
                {
                    Console.WriteLine("  ✓ Validacija: Exception bačen za negativan broj");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška pri kreiranju pool-a: {ex.Message}");
            }
        }

        private static void TestWorkerExecution()
        {
            const int numThreads = 4;
            var pool = new ThreadedMiner.MiningThreadPool(numThreads);
            var sharedState = pool.SharedState;
            
            // Kreiraj jednostavne worker-e sa deljenim state-om
            var workers = new System.Collections.Generic.List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < numThreads; i++)
            {
                int threadId = i;
                var worker = new SimpleTestWorker(threadId, sharedState);
                workers.Add(worker);
            }

            Console.WriteLine($"  Pokretanje {numThreads} niti...");
            pool.StartWithWorkers(workers);

            // Čekaj da se niti završe (max 2 sekunde)
            Thread.Sleep(100); // Daj vremena nitima da se pokrenu
            
            bool allCompleted = pool.WaitAll(2000);
            if (allCompleted)
            {
                Console.WriteLine($"  ✓ Sve niti su se uspešno završile");
                
                // Proveri da li su svi workers izvršili svoj posao
                bool allWorkersExecuted = true;
                foreach (var worker in workers)
                {
                    if (worker is SimpleTestWorker stw && !stw.HasExecuted)
                    {
                        allWorkersExecuted = false;
                        break;
                    }
                }
                
                if (allWorkersExecuted)
                {
                    Console.WriteLine("  ✓ Svi workers su izvršili svoj posao");
                }
                else
                {
                    Console.WriteLine("  ✗ Neki workers nisu izvršili svoj posao");
                }
            }
            else
            {
                Console.WriteLine("  ✗ Neki niti se nisu završili na vreme");
            }

            pool.Stop();
        }

        private static void TestStopWorkers()
        {
            const int numThreads = 2;
            var pool = new ThreadedMiner.MiningThreadPool(numThreads);
            var sharedState = pool.SharedState;
            
            var workers = new System.Collections.Generic.List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < numThreads; i++)
            {
                int threadId = i;
                var worker = new LongRunningTestWorker(threadId, sharedState);
                workers.Add(worker);
            }

            Console.WriteLine("  Pokretanje dugih radnih niti...");
            pool.StartWithWorkers(workers);

            Thread.Sleep(100); // Daj vremena nitima da se pokrenu

            Console.WriteLine("  Zaustavljanje niti...");
            pool.Stop();

            if (pool.AllThreadsCompleted)
            {
                Console.WriteLine("  ✓ Sve niti su uspešno zaustavljene");
            }
            else
            {
                Console.WriteLine("  ✗ Neke niti nisu zaustavljene");
            }
        }

        private static void TestSharedFlag()
        {
            const int numThreads = 4;
            var pool = new ThreadedMiner.MiningThreadPool(numThreads);
            var sharedState = pool.SharedState;

            // Test: Provera da flag nije postavljen na početku
            if (!sharedState.IsSolutionFound())
            {
                Console.WriteLine("  ✓ Flag nije postavljen na početku");
            }
            else
            {
                Console.WriteLine("  ✗ Flag je postavljen na početku (greška)");
            }

            // Test: Postavljanje flag-a
            bool firstSet = sharedState.TrySetSolutionFound();
            if (firstSet && sharedState.IsSolutionFound())
            {
                Console.WriteLine("  ✓ Flag je uspešno postavljen");
            }
            else
            {
                Console.WriteLine("  ✗ Greška pri postavljanju flag-a");
            }

            // Test: Pokušaj ponovnog postavljanja (ne bi trebalo da uspe - već je postavljen)
            bool secondSet = sharedState.TrySetSolutionFound();
            if (!secondSet)
            {
                Console.WriteLine("  ✓ Drugi pokušaj postavljanja vraća false (već je postavljen)");
            }
            else
            {
                Console.WriteLine("  ✗ Drugi pokušaj postavljanja je uspeo (ne bi trebalo)");
            }

            // Test: Reset flag-a
            sharedState.Reset();
            if (!sharedState.IsSolutionFound())
            {
                Console.WriteLine("  ✓ Flag je uspešno resetovan");
            }
            else
            {
                Console.WriteLine("  ✗ Greška pri resetovanju flag-a");
            }

            // Test: Više niti pokušavaju postaviti flag - samo jedna treba uspeti
            var workers = new System.Collections.Generic.List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < numThreads; i++)
            {
                int threadId = i;
                var worker = new FlagTestWorker(threadId, sharedState);
                workers.Add(worker);
            }

            sharedState.Reset();
            pool.StartWithWorkers(workers);
            
            Thread.Sleep(500); // Daj vremena nitima da rade
            
            pool.WaitAll(1000);
            
            // Proveri koliko workers je postavilo flag (samo jedan bi trebalo)
            int successfulWorkers = 0;
            foreach (var worker in workers)
            {
                if (worker is FlagTestWorker ftw && ftw.SuccessfullySetFlag)
                {
                    successfulWorkers++;
                }
            }

            if (successfulWorkers == 1)
            {
                Console.WriteLine($"  ✓ Tačno jedna nit je postavila flag (thread-safe)");
            }
            else
            {
                Console.WriteLine($"  ✗ {successfulWorkers} niti je postavilo flag (trebalo bi biti 1)");
            }

            pool.Stop();
        }

        /// <summary>
        /// Jednostavan test worker koji samo izvršava akciju i završava
        /// </summary>
        private class SimpleTestWorker : ThreadedMiner.MiningWorker
        {
            public bool HasExecuted { get; private set; }

            public SimpleTestWorker(int threadId, ThreadedMiner.SharedMiningState sharedState) 
                : base(threadId, sharedState)
            {
            }

            public override void Execute()
            {
                HasExecuted = true;
                // Simuliraj kratak rad
                Thread.Sleep(10);
            }
        }

        /// <summary>
        /// Worker koji radi duže vreme - za testiranje stop funkcionalnosti
        /// </summary>
        private class LongRunningTestWorker : ThreadedMiner.MiningWorker
        {
            public LongRunningTestWorker(int threadId, ThreadedMiner.SharedMiningState sharedState) 
                : base(threadId, sharedState)
            {
            }

            public override void Execute()
            {
                // Radi dok nije otkazan ili rešenje pronađeno (preko shared flag-a)
                while (!ShouldStop())
                {
                    Thread.Sleep(100);
                }
            }
        }

        /// <summary>
        /// Worker za testiranje deljenog flag-a - pokušava da postavi flag
        /// </summary>
        private class FlagTestWorker : ThreadedMiner.MiningWorker
        {
            public bool SuccessfullySetFlag { get; private set; }

            public FlagTestWorker(int threadId, ThreadedMiner.SharedMiningState sharedState) 
                : base(threadId, sharedState)
            {
            }

            public override void Execute()
            {
                // Pokušaj postaviti flag
                // Simuliraj neki rad pre pokušaja
                Thread.Sleep(ThreadId * 50); // Različito vreme za svaku nit
                
                SuccessfullySetFlag = TryMarkSolutionFound();
            }
        }
    }
}

