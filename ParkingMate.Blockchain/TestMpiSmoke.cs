using System;
using MPI;

namespace ParkingMate.Blockchain
{
    public static class TestMpiSmoke
    {
        // NEMA MPI.Environment ovde. NEMA Communicator.world ovde.
        // Sve dobija spolja iz Program.cs
        public static void Run(Intracommunicator world, IMpiCommunication comm, int threadsPerWorker = 2, uint difficulty = 2)
        {
            if (world.Size < 2)
            {
                if (world.Rank == 0)
                    Console.WriteLine("SMOKE TEST: pokreni sa mpiexec -n 2 (ili vise).");
                return;
            }

            if (world.Rank == 0)
            {
                Console.WriteLine($"[SMOKE MASTER] size={world.Size}");

                var bc = new Blockchain(
                    blockIntervalSeconds: 10,
                    adjustmentInterval: 10,
                    threadCount: 1
                );

                var latest = bc.GetLatestBlock();

                var template = new Block(
                    index: latest.Index + 1,
                    data: "REAL MPI SMOKE",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: latest.Hash,
                    difficulty: difficulty,
                    nonce: 0
                );

                Console.WriteLine("[SMOKE MASTER] start distributed mining...");

                var mined = MpiMining.RunDistributedMiningAsMaster(
                    comm,
                    world.Size,
                    template,
                    threadsPerWorker: threadsPerWorker
                );

                bool ok = bc.IsValidNewBlock(mined, latest);
                Console.WriteLine($"[SMOKE MASTER] mined valid = {ok}");
                Console.WriteLine($"[SMOKE MASTER] nonce={mined.Nonce}, hash={mined.Hash}");

                if (!ok)
                    throw new Exception("SMOKE FAIL: mined block invalid");
            }
            else
            {
                Console.WriteLine($"[SMOKE WORKER {world.Rank}] start");
                MpiMining.RunAsWorker(comm, world.Rank, threadsPerWorker: threadsPerWorker);
                Console.WriteLine($"[SMOKE WORKER {world.Rank}] done");
            }
        }
    }
}
