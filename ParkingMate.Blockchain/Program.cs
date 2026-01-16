using MPI;
using System;

namespace ParkingMate.Blockchain
{
    class Program
    {
        static void Main(string[] args)
        {
            AppContext.SetSwitch("System.Runtime.Serialization.EnableUnsafeBinaryFormatterSerialization", true);

            // Dodatni "mode" flagovi (ne diramo CommandLineArgs, samo ovde čitamo)
            bool smoke = HasFlag(args, "--smoke");
            bool runTests = HasFlag(args, "--run-tests");

            // Parsiranje command-line argumenata (OK je da se uradi pre MPI env-a)
            var cliArgs = CommandLineArgs.Parse(args);

            if (cliArgs.ShowHelp)
            {
                CommandLineArgs.PrintHelp();
                return;
            }



            // Test ThreadPool implementacije (4.1.2, 4.1.3, 4.1.4)
            // Otkomentariši sledeću liniju da testiraš ThreadPool:
            // TestThreadPool.RunTest();
            // return;

            // Test skaliranja (4.1.5)
            // Otkomentariši sledeću liniju da testiraš skaliranje sa različitim brojevima niti:
            //// TestScaling.RunTest();
            // return;

            // Test detekcije CPU jezgara (4.2.1)
            // Otkomentariši sledeću liniju da testiraš detekciju CPU jezgara:
            // TestCpuDetection.RunTest();
            // return;

            // Test MPI inicijalizacije (5.1.1, 5.1.2, 5.1.3)
            // Otkomentariši sledeću liniju da testiraš MPI inicijalizaciju:
            //TestMpiInitialization.RunTest();
            // return;

            // Test MPI Master-Worker arhitekture (5.2.1-5.2.5)
            // Otkomentariši sledeću liniju da testiraš Master-Worker arhitekturu:
            //// TestMpiMasterWorker.RunTest();
            // return;

            // Test MPI sinhronizacije i prekida (5.3.1, 5.3.2, 5.3.3)
            // Otkomentariši sledeću liniju da testiraš MPI sinhronizaciju:
            //// TestMpiSynchronization.RunTest();
            //// return;

            // Test dinamičke težine (6.1.1)
            // Otkomentariši sledeću liniju da testiraš time-based algoritam:
            // TestDynamicDifficulty.RunTest();
            // return;

            // Test parametara dinamičke težine (6.1.2)
            // Otkomentariši sledeću liniju da testiraš CLI parametre:
            //TestDynamicDifficultyParams.RunTest();
            // return;

            // Test integracije dinamičke težine u mining proces (6.1.3)
            // Otkomentariši sledeću liniju da testiraš integraciju:
            //TestDynamicDifficultyIntegration.RunTest();
            //  return;

            // Test izračunavanja kumulativne težine (6.2.1)
            // Otkomentariši sledeću liniju da testiraš izračunavanje 2^difficulty:
            // TestCumulativeWeight.RunTest();
            // return;

            // Test validacije timestamp-a (2.3)
            // Otkomentariši sledeću liniju da testiraš validaciju timestamp-a:
            // TestTimestampValidation.RunTest();
            // return;

            // Test validacije bloka i lanca (Block + Chain validation)
            // Otkomentariši sledeću liniju da testiraš validaciju:
            //TestBlockAndChainValidation.RunTest();
            //return;

            // Test da mining kreira validan blok (Mining + Validation integration)
            // Otkomentariši sledeću liniju da testiraš mining validnog bloka:
            // TestMiningCreatesValidBlock.RunTest();
            // return;

            // Test MPI skaliranja / benchmark (MPI performance test)
            // Otkomentariši sledeću liniju da testiraš MPI scaling i speedup:
            // TestMpiScalingBenchmark.RunTest();
            // return;


            // Dodatni "mode" flagovi (ne diramo CommandLineArgs, samo ovde čitamo)
            int threads = cliArgs.GetThreadCount();
            uint initialDifficulty = cliArgs.GetDifficulty();
            int blocksToMine = cliArgs.GetBlocksToMine();
            long blockIntervalSeconds = cliArgs.GetBlockIntervalSeconds();
            uint adjustmentInterval = cliArgs.GetAdjustmentInterval();

            // MPI.NET environment – sve MPI pozive radi SAMO unutar ovog using-a
            using (new MPI.Environment(ref args))
            {
                var world = Communicator.world;
                IMpiCommunication comm = new RealMpiCommunication(world);

                // 1) Smoke mod (brz sanity check)
                if (smoke)
                {
                    if (world.Rank == 0)
                        Console.WriteLine($"[MODE] SMOKE (size={world.Size})");

                    // difficulty=2, threadsPerWorker=2 (mozes menjati)
                    TestMpiSmoke.Run(world, comm, threadsPerWorker: 2, difficulty: 2);

                    comm.Barrier();
                    return;
                }

                // 2) Lokalni testovi (bez MPI) – samo rank 0 da ne duplira ispise
                if (runTests)
                {
                    if (world.Rank == 0)
                    {
                        Console.WriteLine("[MODE] RUN TESTS (local tests on rank 0)");
                        TestMiningCreatesValidBlock.RunTest();
                        TestBlockAndChainValidation.RunTest();
                        Console.WriteLine("[TESTS] DONE.");
                    }

                    comm.Barrier();
                    return;
                }

                // 3) Full mining (lokalno ili MPI)
                bool mpiMode = world.Size > 1;

                if (!mpiMode)
                {
                    Console.WriteLine("MPI size = 1 -> radim lokalno (bez distribuiranja).");

                    var blockchain = new Blockchain(blockIntervalSeconds, adjustmentInterval, threads);
                    uint currentDifficulty = initialDifficulty;

                    for (int i = 1; i <= blocksToMine; i++)
                    {
                        currentDifficulty = blockchain.GetNextDifficulty(currentDifficulty);

                        var block = new Block(
                            index: 0,
                            data: $"Auto-mined block #{i}",
                            timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                            previousHash: "",
                            difficulty: currentDifficulty,
                            nonce: 0
                        );

                        blockchain.AddBlock(block);
                        var latest = blockchain.GetLatestBlock();
                        Console.WriteLine($"[LOCAL] Added block {latest.Index} diff={latest.Difficulty} hash={latest.Hash.Substring(0, 16)}...");
                    }

                    Console.WriteLine("Blockchain valid: " + blockchain.IsValidChain());
                    return;
                }

                // MPI mining
                // MPI mining
                if (world.Rank == 0)
                {
                    Console.WriteLine($"[MASTER] MPI size={world.Size}, threads/worker={threads}");

                    var blockchain = new Blockchain(blockIntervalSeconds, adjustmentInterval, threadCount: 1);
                    uint currentDifficulty = initialDifficulty;

                    for (int i = 1; i <= blocksToMine; i++)
                    {
                        currentDifficulty = blockchain.GetNextDifficulty(currentDifficulty);

                        var latest = blockchain.GetLatestBlock();
                        long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                        long ts = Math.Max(now, latest.Timestamp + 1); // ✅ garantuje da vreme raste

                        var blockTemplate = new Block(
                            index: latest.Index + 1,
                            data: $"MPI-mined block #{i}",
                            timestamp: ts,
                            previousHash: latest.Hash,
                            difficulty: currentDifficulty,
                            nonce: 0
                        );


                        Console.WriteLine($"[MASTER] Mining block {i}/{blocksToMine} difficulty={currentDifficulty} ...");

                        Block mined = MpiMining.RunDistributedMiningAsMaster(
                            comm,
                            world.Size,
                            blockTemplate,
                            threadsPerWorker: threads
                        );

                        if (!blockchain.IsValidNewBlock(mined, latest))
                            throw new InvalidOperationException("MPI mined block is invalid");

                        blockchain.AppendMinedBlock(mined);
                        Console.WriteLine($"[MASTER] Added block {mined.Index} hash={mined.Hash.Substring(0, 16)}...");
                    }

                    Console.WriteLine("[MASTER] Done mining. Sending shutdown jobs...");

                    MpiMining.SendShutdownToWorkers(comm, world.Size);

                    Console.WriteLine("[MASTER] Shutdown sent. Chain valid: " + blockchain.IsValidChain());
                }
                else
                {
                    Console.WriteLine($"[WORKER {world.Rank}] Starting. threads={threads}");

                    while (true)
                    {
                        bool shouldExit = MpiMining.RunAsWorker(comm, world.Rank, threadsPerWorker: threads);
                        if (shouldExit) break; // izlaz samo kad dobije shutdown
                    }

                    Console.WriteLine($"[WORKER {world.Rank}] Done.");

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
