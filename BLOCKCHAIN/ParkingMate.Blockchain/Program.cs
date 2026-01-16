using MPI;
using System;

namespace ParkingMate.Blockchain
{
    class Program
    {
        static void Main(string[] args)
        {
            AppContext.SetSwitch(
                "System.Runtime.Serialization.EnableUnsafeBinaryFormatterSerialization",
                true
            );

            // =========================
            // MODE FLAGS
            // =========================
            bool bench = HasFlag(args, "--bench");

            // =========================
            // CLI PARSING (OK pre MPI)
            // =========================
            var cliArgs = CommandLineArgs.Parse(args);

            if (cliArgs.ShowHelp)
            {
                CommandLineArgs.PrintHelp();
                return;
            }

            // =========================================================
            // SVI OSTALI TESTOVI — OSTAVLJENI, ALI ISKLJUČENI
            // =========================================================

            /*
            TestDynamicDifficulty.RunTest();
            TestDynamicDifficultyParams.RunTest();
            TestBlockAndChainValidation.RunTest();
            TestMiningCreatesValidBlock.RunTest();
            TestCumulativeWeight.RunTest();
            TestTimestampValidation.RunTest();
            TestMpiInitialization.RunTest();
            TestMpiMasterWorker.RunTest();
            TestMpiSynchronization.RunTest();
            */

            // =========================================================
            // MPI ENVIRONMENT (SVE MPI IDE OVDE)
            // =========================================================
            using (new MPI.Environment(ref args))
            {
                var world = Communicator.world;
                IMpiCommunication comm = new RealMpiCommunication(world);

                // =====================================================
                // MPI BENCHMARK MODE (ZA GRAFOVE)
                // =====================================================
                if (bench)
                {
                    if (world.Rank == 0)
                    {
                        Console.WriteLine("=======================================");
                        Console.WriteLine(" MPI SCALING BENCHMARK MODE ");
                        Console.WriteLine("=======================================");
                        Console.WriteLine($"World size: {world.Size}");
                        Console.WriteLine();
                    }

                    TestMpiScalingBenchmark.Run(world, comm);
                    comm.Barrier();
                    return;
                }

                // =====================================================
                // DEFAULT: NIŠTA NE RADI (DA SE SLUČAJNO NE MINE)
                // =====================================================
                if (world.Rank == 0)
                {
                    Console.WriteLine("Nijedan mode nije izabran.");
                    Console.WriteLine("Za benchmark koristi:");
                    Console.WriteLine("  mpiexec -n <N> dotnet run -- --bench");
                }

                comm.Barrier();
            }
        }

        private static bool HasFlag(string[] args, string flag)
        {
            if (args == null) return false;
            foreach (var a in args)
                if (string.Equals(a, flag, StringComparison.OrdinalIgnoreCase))
                    return true;
            return false;
        }
    }
}
