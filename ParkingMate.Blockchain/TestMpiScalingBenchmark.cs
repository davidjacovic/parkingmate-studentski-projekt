using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Globalization;
using System.IO;
using MPI;

namespace ParkingMate.Blockchain
{
    public static class TestMpiScalingBenchmark
    {
        // Podešavanja benchmark-a
        private const int TAG_BENCH = 999;

        /// <summary>
        /// Benchmark MPI mining-a: meri vreme rudarjenja 1 bloka za više threadCount vrednosti.
        /// Pokreće se pod MPI (mpiexec -n X).
        ///
        /// Rezultat: rank 0 ispisuje tabelu + snima CSV "mpi_benchmark_rank0.csv"
        /// </summary>
        public static void Run(Intracommunicator world, IMpiCommunication comm)
        {
            if (world == null) throw new ArgumentNullException(nameof(world));
            if (comm == null) throw new ArgumentNullException(nameof(comm));

            // U praksi: uzmi nekoliko thread-count vrednosti (ne previše)
            int[] threadOptions = new[] { 1, 2, 4, 8, 16 };

            // Težina: dovoljno velika da meriš (ali da ne traje predugo)
            // Ako ti je prebrzo (npr. < 10ms), podigni difficulty na 4 ili 5.
            uint difficulty = 4;

            // Ponovi više puta pa uzmi prosek
            int repeats = 5;

            // Rank 0 pravi template blok (svi procesi moraju da rade isti posao u svakoj iteraciji)
            // Timestamp mora da bude validan: u master-u već imaš logiku da osigura rast vremena.
            // Ovde ćemo to uraditi ručno na rank0, a workerima se šalje kroz NonceRangeMessage.
            var results = new List<(int worldSize, int threads, double avgMs)>();

            if (world.Size == 1)
            {
                // Nije MPI run, ali može da posluži kao baseline local.
                if (world.Rank == 0)
                    Console.WriteLine("[BENCH] world.Size == 1 (no MPI). Pokreni sa mpiexec -n X za MPI benchmark.");
                return;
            }

            if (world.Rank == 0)
            {
                Console.WriteLine("=== MPI Scaling Benchmark ===");
                Console.WriteLine($"worldSize={world.Size} (master+{world.Size - 1} workers), difficulty={difficulty}, repeats={repeats}");
                Console.WriteLine();
            }

            // Barijera da svi startuju sinhrono
            comm.Barrier();

            // Da bi timestamp pravila bila stabilna, držimo mali blockchain samo na masteru.
            // Workerima šaljemo već izračunat template.
            Blockchain masterChain = new Blockchain(blockIntervalSeconds: 600, adjustmentInterval: 10, threadCount: 1);

            // Benchmark po broju niti
            foreach (int threadsPerWorker in threadOptions)
            {
                // Preskoči prevelike vrednosti (da ne praviš 1000 threadova na malom CPU)
                if (threadsPerWorker <= 0) continue;

                // Warmup + avg
                double sumMs = 0;

                for (int r = 0; r < repeats; r++)
                {
                    comm.Barrier();

                    if (world.Rank == 0)
                    {
                        var latest = masterChain.GetLatestBlock();
                        long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                        long ts = Math.Max(now, latest.Timestamp + 1);

                        var template = new Block(
                            index: latest.Index + 1,
                            data: $"BENCH block (threads={threadsPerWorker}, rep={r})",
                            timestamp: ts,
                            previousHash: latest.Hash,
                            difficulty: difficulty,
                            nonce: 0
                        );

                        var sw = Stopwatch.StartNew();

                        // Master distribuira posao i čeka rezultate
                        Block mined = MpiMining.RunDistributedMiningAsMaster(
                            comm,
                            world.Size,
                            template,
                            threadsPerWorker: threadsPerWorker
                        );

                        sw.Stop();

                        // Validacija i append (da sledeći template ima smislen previousHash/timestamp)
                        if (!masterChain.IsValidNewBlock(mined, latest))
                            throw new InvalidOperationException("[BENCH] Mined block invalid (timestamp/hash/pow).");

                        masterChain.AppendMinedBlock(mined);

                        sumMs += sw.Elapsed.TotalMilliseconds;
                    }
                    else
                    {
                        // Worker: uradi jednu rundu pa se vrati (RunAsWorker čeka STOP, pa vraća false)
                        // NAPOMENA: master šalje STOP u svakoj rundi.
                        bool shouldExit = MpiMining.RunAsWorker(comm, world.Rank, threadsPerWorker);
                        if (shouldExit)
                            throw new InvalidOperationException("[BENCH] Worker got shutdown during benchmark (unexpected).");
                    }

                    comm.Barrier();
                }

                if (world.Rank == 0)
                {
                    double avg = sumMs / repeats;
                    results.Add((world.Size, threadsPerWorker, avg));
                    Console.WriteLine($"threads/worker={threadsPerWorker,-3} avgTime={avg,8:F2} ms");
                }

                comm.Barrier();
            }

            // Po završetku benchmark-a: master pošalje shutdown da worker petlje mogu da se završe ako želiš.
            // (Ovo je bezbedno i korisno ako posle benchmark-a završavaš program.)
            if (world.Rank == 0)
                MpiMining.SendShutdownToWorkers(comm, world.Size);

            comm.Barrier();

            if (world.Rank == 0)
            {
                // Računaj speedup u okviru ovog worldSize: baseline = threads=1
                double baseline = -1;
                foreach (var row in results)
                    if (row.threads == 1) { baseline = row.avgMs; break; }

                Console.WriteLine();
                Console.WriteLine("CSV columns: worldSize,threadsPerWorker,avgMs,speedup_vs_threads1");
                string csvPath = "mpi_benchmark_rank0.csv";

                using var sw = new StreamWriter(csvPath);
                sw.WriteLine("worldSize,threadsPerWorker,avgMs,speedup_vs_threads1");

                foreach (var row in results)
                {
                    double speedup = (baseline > 0) ? (baseline / row.avgMs) : 0;
                    sw.WriteLine(string.Join(",",
                        row.worldSize.ToString(CultureInfo.InvariantCulture),
                        row.threads.ToString(CultureInfo.InvariantCulture),
                        row.avgMs.ToString("F4", CultureInfo.InvariantCulture),
                        speedup.ToString("F4", CultureInfo.InvariantCulture)
                    ));
                }

                Console.WriteLine($"Saved: {csvPath}");
                Console.WriteLine();
                Console.WriteLine("Kako za graf 'speedup vs #procesa'?");
                Console.WriteLine("  Pokreni isti benchmark sa mpiexec -n 2,4,8... i uporedi rezultate (npr. uzmi threads=optimal).");
            }
        }
    }
}
