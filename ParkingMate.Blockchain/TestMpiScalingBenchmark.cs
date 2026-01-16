using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Performansni benchmark: speedup po broju "MPI procesa" (simulirano) i niti po procesu.
    /// OVO JE BENCHMARK (nije funkcionalni test).
    /// </summary>
    public static class TestMpiScalingBenchmark
    {
        public class Result
        {
            public int Processes { get; set; }
            public int ThreadsPerProcess { get; set; }
            public long TimeMs { get; set; }
            public double Speedup { get; set; }
            public ulong Nonce { get; set; }
            public string HashPrefix { get; set; } = "";
        }

        public static void RunTest()
        {
            Console.WriteLine("=== Test MPI Scaling Benchmark (performansni) ===\n");

            // Podešavanja (za demo/ppt):
            uint difficulty = 4; // spusti na 3 ako je presporo na tvojoj mašini
            int threadsPerProc = Math.Max(1, ThreadedMiner.GetOptimalThreadCountMaxPerformance());

            // Broj “procesa” (čvorova)
            int[] processCounts = { 1, 2, 4, 8 };

            Console.WriteLine($"difficulty={difficulty}, threadsPerProcess={threadsPerProc}\n");

            // Baseline je P=1
            var results = new List<Result>();
            long baselineMs = 0;

            foreach (var p in processCounts)
            {
                Console.WriteLine($"--- Run: processes={p} ---");

                var sw = Stopwatch.StartNew();
                var mined = MineDistributedSimulated(processes: p, threadsPerProcess: threadsPerProc, difficulty: difficulty);
                sw.Stop();

                if (mined == null)
                {
                    Console.WriteLine("  ⚠ Nije pronađeno rešenje (vrlo retko na niskoj diff, ali moguće).");
                    continue;
                }

                long ms = sw.ElapsedMilliseconds;
                if (p == 1) baselineMs = Math.Max(1, ms);

                var r = new Result
                {
                    Processes = p,
                    ThreadsPerProcess = threadsPerProc,
                    TimeMs = ms,
                    Speedup = (baselineMs > 0) ? (double)baselineMs / Math.Max(1, ms) : 1.0,
                    Nonce = mined.Nonce,
                    HashPrefix = mined.Hash.Substring(0, Math.Min(12, mined.Hash.Length))
                };
                results.Add(r);

                Console.WriteLine($"  time={ms} ms, nonce={r.Nonce:N0}, hashPrefix={r.HashPrefix}..., speedup={r.Speedup:F2}x\n");
            }

            PrintTable(results);

            Console.WriteLine("Napomena: Ovo je 'simulirani MPI' benchmark (procesi=Task), dobar za grafove u prezentaciji.\n");
        }

        private static Block? MineDistributedSimulated(int processes, int threadsPerProcess, uint difficulty)
        {
            // Zajednički blok (svaki proces dobija kopiju)
            var baseBlock = new Block(
                index: 1,
                data: $"MPI scaling benchmark P={processes}",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: difficulty,
                nonce: 0
            );

            // Podeli nonce prostor na P procesa:
            var ranges = DivideNonceSpace(processes);

            // Pokreni procese paralelno
            var tasks = new List<Task<Block?>>(processes);

            for (int rank = 0; rank < processes; rank++)
            {
                int localRank = rank;
                var (start, end) = ranges[localRank];

                tasks.Add(Task.Run(() =>
                {
                    // Svaki "proces" radi multi-thread mining u svom nonce opsegu.
                    var copy = new Block(
                        baseBlock.Index,
                        baseBlock.Data,
                        baseBlock.Timestamp,
                        baseBlock.PreviousHash,
                        baseBlock.Difficulty,
                        baseBlock.Nonce
                    );

                    return MineInRangeWithThreads(copy, threadsPerProcess, start, end);
                }));
            }

            // Čekaj prvog koji uspe
            while (tasks.Count > 0)
            {
                int idx = Task.WaitAny(tasks.ToArray());
                var finished = tasks[idx];
                tasks.RemoveAt(idx);

                var candidate = finished.Result;
                if (candidate != null)
                {
                    // našli smo rešenje; ostali se mogu ignorisati (benchmark)
                    return candidate;
                }
            }

            return null;
        }

        private static Block? MineInRangeWithThreads(Block blockToMine, int threadCount, ulong rangeStart, ulong rangeEnd)
        {
            // U okviru procesa podeli njegov nonce range na niti:
            var pool = new ThreadedMiner.MiningThreadPool(threadCount);
            var shared = pool.SharedState;

            var workers = new List<ThreadedMiner.MiningWorker>(threadCount);

            // Deli [rangeStart, rangeEnd] na threadCount delova
            ulong total = (rangeEnd > rangeStart) ? (rangeEnd - rangeStart) : 0;
            ulong chunk = total / (ulong)threadCount;

            for (int i = 0; i < threadCount; i++)
            {
                ulong start = rangeStart + (ulong)i * chunk;
                ulong end = (i == threadCount - 1) ? rangeEnd : (rangeStart + (ulong)(i + 1) * chunk);

                var copy = new Block(
                    blockToMine.Index,
                    blockToMine.Data,
                    blockToMine.Timestamp,
                    blockToMine.PreviousHash,
                    blockToMine.Difficulty,
                    0
                );

                var w = new ThreadedMiner.BlockMiningWorker(
                    threadId: i,
                    sharedState: shared,
                    blockToMine: copy,
                    startNonce: start,
                    endNonce: end
                );
                workers.Add(w);
            }

            pool.StartWithWorkers(workers);
            pool.WaitAll();
            var found = shared.GetFoundBlock();
            pool.Stop();
            return found;
        }

        private static List<(ulong start, ulong end)> DivideNonceSpace(int processes)
        {
            // Podela kao tvoj CalculateNonceRange, ali za procese.
            // (Isto pravilo: poslednji dobija do MaxValue)
            var ranges = new List<(ulong, ulong)>(processes);
            ulong max = ulong.MaxValue;
            ulong step = max / (ulong)processes;

            for (int p = 0; p < processes; p++)
            {
                ulong start = (ulong)p * step;
                ulong end = (p == processes - 1) ? ulong.MaxValue : (ulong)(p + 1) * step;
                ranges.Add((start, end));
            }

            return ranges;
        }

        private static void PrintTable(List<Result> results)
        {
            if (results.Count == 0)
            {
                Console.WriteLine("Nema rezultata.\n");
                return;
            }

            Console.WriteLine("=== Rezultati (baseline P=1) ===");
            Console.WriteLine($"{"P",3} {"Threads/P",9} {"Time (ms)",10} {"Speedup",8} {"Nonce",16} {"HashPrefix",12}");
            Console.WriteLine(new string('-', 65));

            foreach (var r in results.OrderBy(x => x.Processes))
            {
                Console.WriteLine($"{r.Processes,3} {r.ThreadsPerProcess,9} {r.TimeMs,10:N0} {r.Speedup,8:F2} {r.Nonce,16:N0} {r.HashPrefix,12}");
            }

            Console.WriteLine();
        }
    }
}
