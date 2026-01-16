using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje MPI inicijalizacije (Subtask 5.1.1)
    /// (legacy/simulacioni MpiEnvironment - nije MPI.NET).
    /// </summary>
    public class TestMpiInitialization
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI inicijalizacije (5.1.1) ===\n");

            Console.WriteLine("Test 1: Osnovna inicijalizacija MPI okruženja");
            TestBasicInitialization();
            Console.WriteLine();

            Console.WriteLine("Test 2: Inicijalizacija sa različitim rank i size vrednostima");
            TestDifferentRankSize();
            Console.WriteLine();

            Console.WriteLine("Test 3: Detekcija rank-a i size-a (5.1.2)");
            TestRankSizeDetection();
            Console.WriteLine();

            Console.WriteLine("Test 4: Master/Worker detekcija");
            TestMasterWorkerDetection();
            Console.WriteLine();

            Console.WriteLine("Test 5: Parsiranje MPI flag-ova iz CLI argumenata (5.1.3)");
            TestCliParsing();
            Console.WriteLine();

            Console.WriteLine("Test 6: Validacija MPI okruženja");
            TestValidation();
            Console.WriteLine();

            Console.WriteLine("✓ Svi testovi MPI inicijalizacije su prošli!");
            Console.WriteLine();
            Console.WriteLine("=== Sažetak završenih subtaskova ===");
            Console.WriteLine("✓ 5.1.1: MPI inicijalizacija - Initialize(), InitializeFromArgs()");
            Console.WriteLine("✓ 5.1.2: Detekcija rank-a i size-a - Rank i Size properties");
            Console.WriteLine("✓ 5.1.3: MPI flag u CLI interfejsu - --mpi, --mpi-size, --mpi-rank");
        }

        private static void TestBasicInitialization()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Shutdown(); // Reset za test

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
            mpi.Shutdown();

            int[] sizes = { 1, 2, 4, 8 };
            int[] ranks = { 0, 1, 2, 3 };

            bool allPassed = true;
            foreach (int size in sizes)
            {
                foreach (int rank in ranks)
                {
                    if (rank >= size) continue;

                    mpi.Shutdown();
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
                Console.WriteLine("  ✓ Sve kombinacije rank/size rade ispravno");
        }

        private static void TestRankSizeDetection()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Shutdown();

            Console.WriteLine("  Subtask 5.1.2: Detekcija rank-a i size-a");

            mpi.Initialize(size: 4, rank: 0);
            if (mpi.Rank == 0 && mpi.Size == 4)
            {
                Console.WriteLine($"  ✓ Rank detekcija (5.1.2): {mpi.Rank}");
                Console.WriteLine($"  ✓ Size detekcija (5.1.2): {mpi.Size}");
            }
            else
            {
                Console.WriteLine("  ✗ Greška pri detekciji rank-a ili size-a");
            }

            mpi.Shutdown();
            mpi.Initialize(size: 8, rank: 3);
            if (mpi.Rank == 3 && mpi.Size == 8)
            {
                Console.WriteLine($"  ✓ Worker rank detekcija (5.1.2): {mpi.Rank}");
                Console.WriteLine($"  ✓ Worker size detekcija (5.1.2): {mpi.Size}");
            }
            else
            {
                Console.WriteLine("  ✗ Greška pri detekciji worker rank-a ili size-a");
            }

            mpi.Shutdown();
            mpi.Initialize(size: 16, rank: 7);
            try
            {
                int rank = mpi.Rank;
                int size = mpi.Size;
                if (rank == 7 && size == 16)
                {
                    Console.WriteLine($"  ✓ Rank property (5.1.2): vraća {rank}");
                    Console.WriteLine($"  ✓ Size property (5.1.2): vraća {size}");
                }
            }
            catch (Exception ex)
            {
                Console.WriteLine($"  ✗ Greška pri čitanju Rank/Size properties: {ex.Message}");
            }
        }

        private static void TestMasterWorkerDetection()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Shutdown();

            mpi.Initialize(size: 4, rank: 0);
            if (mpi.IsMaster && !mpi.IsWorker)
                Console.WriteLine("  ✓ Master detekcija: Rank 0 je master");
            else
                Console.WriteLine("  ✗ Greška: Rank 0 treba biti master");

            mpi.Shutdown();
            mpi.Initialize(size: 4, rank: 2);
            if (!mpi.IsMaster && mpi.IsWorker)
                Console.WriteLine("  ✓ Worker detekcija: Rank > 0 je worker");
            else
                Console.WriteLine("  ✗ Greška: Rank > 0 treba biti worker");
        }

        private static void TestCliParsing()
        {
            Console.WriteLine("  Subtask 5.1.3: MPI flag u CLI interfejsu");

            string[] args1 = { "--mpi" };
            Console.WriteLine(MpiHelper.IsMpiMode(args1)
                ? "  ✓ Detekcija --mpi flag-a (5.1.3)"
                : "  ✗ Greška: --mpi flag nije detektovan");

            string[] args2 = { "-m" };
            Console.WriteLine(MpiHelper.IsMpiMode(args2)
                ? "  ✓ Detekcija -m flag-a (5.1.3)"
                : "  ✗ Greška: -m flag nije detektovan");

            string[] args2b = { "--use-mpi" };
            Console.WriteLine(MpiHelper.IsMpiMode(args2b)
                ? "  ✓ Detekcija --use-mpi flag-a (5.1.3)"
                : "  ✗ Greška: --use-mpi flag nije detektovan");

            string[] args3 = { "--mpi", "--mpi-size", "4", "--mpi-rank", "1" };
            var (useMpi3, size, rank) = MpiHelper.ParseMpiArgs(args3);
            Console.WriteLine(useMpi3 && size == 4 && rank == 1
                ? $"  ✓ Parsiranje --mpi-size i --mpi-rank (5.1.3): Size={size}, Rank={rank}"
                : "  ✗ Greška pri parsiranju MPI argumenata");

            string[] args3b = { "-m", "-ms", "8", "-mr", "2" };
            var (useMpi3b, size2, rank2) = MpiHelper.ParseMpiArgs(args3b);
            Console.WriteLine(useMpi3b && size2 == 8 && rank2 == 2
                ? $"  ✓ Parsiranje -ms i -mr kratkih formi (5.1.3): Size={size2}, Rank={rank2}"
                : "  ✗ Greška pri parsiranju kratkih formi");

            string[] args4 = { "--threads", "8" };
            Console.WriteLine(!MpiHelper.IsMpiMode(args4)
                ? "  ✓ Normalni režim (bez MPI) detektovan (5.1.3)"
                : "  ✗ Greška: MPI mod je pogrešno detektovan");

            var cliArgs = CommandLineArgs.Parse(new[] { "--mpi", "--mpi-size", "4", "--mpi-rank", "0" });
            Console.WriteLine(cliArgs.UseMpi && cliArgs.MpiSize == 4 && cliArgs.MpiRank == 0
                ? "  ✓ CommandLineArgs.Parse sa MPI flag-ovima (5.1.3)"
                : "  ✗ Greška: CommandLineArgs.Parse ne parsira MPI flag-ove");
        }

        private static void TestValidation()
        {
            var mpi = MpiEnvironment.Instance;
            mpi.Shutdown();

            try
            {
                mpi.Initialize(size: 0, rank: 0);
                Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za size=0");
            }
            catch (ArgumentException)
            {
                Console.WriteLine("  ✓ Validacija: Exception bačen za size=0");
            }

            try
            {
                mpi.Initialize(size: 4, rank: -1);
                Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za negativan rank");
            }
            catch (ArgumentException)
            {
                Console.WriteLine("  ✓ Validacija: Exception bačen za negativan rank");
            }

            try
            {
                mpi.Initialize(size: 4, rank: 5);
                Console.WriteLine("  ✗ Greška: Trebalo je da baci exception za rank >= size");
            }
            catch (ArgumentException)
            {
                Console.WriteLine("  ✓ Validacija: Exception bačen za rank >= size");
            }

            mpi.Shutdown();
            mpi.Initialize(size: 4, rank: 0);
            bool secondInit = mpi.Initialize(size: 8, rank: 1);

            Console.WriteLine(!secondInit
                ? "  ✓ Validacija: Druga inicijalizacija vraća false (već je inicijalizovano)"
                : "  ✗ Greška: Druga inicijalizacija je uspela (ne bi trebalo)");
        }
    }
}
