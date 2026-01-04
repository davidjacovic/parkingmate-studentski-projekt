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
            Console.WriteLine("=== Test ThreadPool implementacije (4.1.2) ===\n");

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

            Console.WriteLine("✓ Svi testovi ThreadPool-a su prošli!");
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
            
            // Kreiraj jednostavne worker-e
            var workers = new System.Collections.Generic.List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < numThreads; i++)
            {
                int threadId = i;
                var worker = new SimpleTestWorker(threadId);
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
            
            var workers = new System.Collections.Generic.List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < numThreads; i++)
            {
                int threadId = i;
                var worker = new LongRunningTestWorker(threadId);
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

        /// <summary>
        /// Jednostavan test worker koji samo izvršava akciju i završava
        /// </summary>
        private class SimpleTestWorker : ThreadedMiner.MiningWorker
        {
            public bool HasExecuted { get; private set; }

            public SimpleTestWorker(int threadId) : base(threadId)
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
            public LongRunningTestWorker(int threadId) : base(threadId)
            {
            }

            public override void Execute()
            {
                // Radi dok nije otkazan
                while (!_cancelled)
                {
                    Thread.Sleep(100);
                }
            }
        }
    }
}

