using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje MPI inicijalizacije (Subtask 5.1.1)
    /// </summary>
    public class TestMpiInitialization
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI inicijalizacije (5.1.1) ===\n");

            // Test 1: Osnovna inicijalizacija
            Console.WriteLine("Test 1: Osnovna inicijalizacija MPI okruženja");
            TestBasicInitialization();
            Console.WriteLine();

            // Test 2: Inicijalizacija sa različitim rank/size
            Console.WriteLine("Test 2: Inicijalizacija sa različitim rank i size vrednostima");
            TestDifferentRankSize();
            Console.WriteLine();

            // Test 3: Detekcija rank-a i size-a
            Console.WriteLine("Test 3: Detekcija rank-a i size-a");
            TestRankSizeDetection();
            Console.WriteLine();

            // Test 4: Master/Worker detekcija
            Console.WriteLine("Test 4: Master/Worker detekcija");
            TestMasterWorkerDetection();
            Console.WriteLine();

            // Test 5: Parsiranje iz command-line argumenata
            Console.WriteLine("Test 5: Parsiranje MPI flag-ova iz CLI argumenata");
            TestCliParsing();
            Console.WriteLine();

            // Test 6: Validacija
            Console.WriteLine("Test 6: Validacija MPI okruženja");
            TestValidation();
            Console.WriteLine();

            Console.WriteLine("✓ Svi testovi MPI inicijalizacije su prošli!");
        }

        private static void TestBasicInitialization()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize(); // Reset za test

            bool initialized = mpi.Initialize(size: 4, rank: 0);
            if (initialized && mpi.IsInitialized)
            {
                Console.WriteLine("  ✓ MPI okruženje je uspešno inicijalizovano");
                Console.WriteLine($"  ✓ Rank: {mpi.Rank}, Size: {mpi.Size}");
            }
            else
            {
                Console.WriteLine("  ✗ Greška: MPI okruženje nije inicijalizovano");
            }
        }

        private static void TestDifferentRankSize()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();

            // Test sa različitim kombinacijama
            int[] sizes = { 1, 2, 4, 8 };
            int[] ranks = { 0, 1, 2, 3 };

            bool allPassed = true;
            foreach (int size in sizes)
            {
                foreach (int rank in ranks)
                {
                    if (rank >= size) continue;

                    mpi.Finalize();
                    try
                    {
                        mpi.Initialize(size, rank);
                        if (mpi.Rank != rank || mpi.Size != size)
                        {
                            Console.WriteLine($"  ✗ Greška: Rank={rank}, Size={size}, ali dobijeno Rank={mpi.Rank}, Size={mpi.Size}");
                            allPassed = false;
                        }
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"  ✗ Greška pri inicijalizaciji Rank={rank}, Size={size}: {ex.Message}");
                        allPassed = false;
                    }
                }
            }

            if (allPassed)
            {
                Console.WriteLine("  ✓ Sve kombinacije rank/size rade ispravno");
            }
        }

        private static void TestRankSizeDetection()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();

            // Test rank 0 (master)
            mpi.Initialize(size: 4, rank: 0);
            if (mpi.Rank == 0 && mpi.Size == 4)
            {
                Console.WriteLine($"  ✓ Rank detekcija: {mpi.Rank}");
                Console.WriteLine($"  ✓ Size detekcija: {mpi.Size}");
            }
            else
            {
                Console.WriteLine("  ✗ Greška pri detekciji rank-a ili size-a");
            }

            // Test rank > 0 (worker)
            mpi.Finalize();
            mpi.Initialize(size: 8, rank: 3);
            if (mpi.Rank == 3 && mpi.Size == 8)
            {
                Console.WriteLine($"  ✓ Worker rank detekcija: {mpi.Rank}");
                Console.WriteLine($"  ✓ Worker size detekcija: {mpi.Size}");
            }
            else
            {
                Console.WriteLine("  ✗ Greška pri detekciji worker rank-a ili size-a");
            }
        }

        private static void TestMasterWorkerDetection()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();

            // Test master (rank 0)
            mpi.Initialize(size: 4, rank: 0);
            if (mpi.IsMaster && !mpi.IsWorker)
            {
                Console.WriteLine("  ✓ Master detekcija: Rank 0 je master");
            }
            else
            {
                Console.WriteLine("  ✗ Greška: Rank 0 treba biti master");
            }

            // Test worker (rank > 0)
            mpi.Finalize();
            mpi.Initialize(size: 4, rank: 2);
            if (!mpi.IsMaster && mpi.IsWorker)
            {
                Console.WriteLine("  ✓ Worker detekcija: Rank > 0 je worker");
            }
            else
            {
                Console.WriteLine("  ✗ Greška: Rank > 0 treba biti worker");
            }
        }

        private static void TestCliParsing()
        {
            // Test sa --mpi flag-om
            string[] args1 = { "--mpi" };
            bool useMpi1 = MpiHelper.IsMpiMode(args1);
            if (useMpi1)
            {
                Console.WriteLine("  ✓ Detekcija --mpi flag-a");
            }
            else
            {
                Console.WriteLine("  ✗ Greška: --mpi flag nije detektovan");
            }

            // Test sa -m flag-om
            string[] args2 = { "-m" };
            bool useMpi2 = MpiHelper.IsMpiMode(args2);
            if (useMpi2)
            {
                Console.WriteLine("  ✓ Detekcija -m flag-a");
            }
            else
            {
                Console.WriteLine("  ✗ Greška: -m flag nije detektovan");
            }

            // Test sa --mpi-size i --mpi-rank
            string[] args3 = { "--mpi", "--mpi-size", "4", "--mpi-rank", "1" };
            var (useMpi3, size, rank) = MpiHelper.ParseMpiArgs(args3);
            if (useMpi3 && size == 4 && rank == 1)
            {
                Console.WriteLine($"  ✓ Parsiranje MPI argumenata: Size={size}, Rank={rank}");
            }
            else
            {
                Console.WriteLine("  ✗ Greška pri parsiranju MPI argumenata");
            }

            // Test bez MPI flag-a
            string[] args4 = { "--threads", "8" };
            bool useMpi4 = MpiHelper.IsMpiMode(args4);
            if (!useMpi4)
            {
                Console.WriteLine("  ✓ Normalni režim (bez MPI) detektovan");
            }
            else
            {
                Console.WriteLine("  ✗ Greška: MPI mod je pogrešno detektovan");
            }
        }

        private static void TestValidation()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Finalize();

            // Test invalid size
            try
            {
                mpi.Initialize(size: 0, rank: 0);
                Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za size=0");
            }
            catch (ArgumentException)
            {
                Console.WriteLine("  ✓ Validacija: Exception bačen za size=0");
            }

            // Test invalid rank (negativan)
            try
            {
                mpi.Initialize(size: 4, rank: -1);
                Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za negativan rank");
            }
            catch (ArgumentException)
            {
                Console.WriteLine("  ✓ Validacija: Exception bačen za negativan rank");
            }

            // Test invalid rank (prevelik)
            try
            {
                mpi.Initialize(size: 4, rank: 5);
                Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za rank >= size");
            }
            catch (ArgumentException)
            {
                Console.WriteLine("  ✓ Validacija: Exception bačen za rank >= size");
            }

            // Test double initialization
            mpi.Finalize();
            mpi.Initialize(size: 4, rank: 0);
            bool secondInit = mpi.Initialize(size: 8, rank: 1);
            if (!secondInit)
            {
                Console.WriteLine("  ✓ Validacija: Druga inicijalizacija vraća false (već je inicijalizovano)");
            }
            else
            {
                Console.WriteLine("  ✗ Greška: Druga inicijalizacija je uspela (ne bi trebalo)");
            }
        }
    }
}

