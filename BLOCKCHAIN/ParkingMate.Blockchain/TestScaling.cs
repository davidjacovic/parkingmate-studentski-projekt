using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje skaliranja paralelnog rudarjenja (Subtask 4.1.5)
    /// Testira performanse sa različitim brojevima niti: 2, 4, 8, 16, 32
    /// </summary>
    public class TestScaling
    {
        public struct ScalingResult
        {
            public int ThreadCount { get; set; }
            public long MiningTimeMs { get; set; }
            public long TotalAttempts { get; set; }
            public Block? FoundBlock { get; set; }
            public int FoundByThreadId { get; set; }
            public double Speedup { get; set; } // Ubrzanje u odnosu na 1 nit
        }

        public static void RunTest()
        {
            Console.WriteLine("=== Test skaliranja paralelnog rudarjenja (4.1.5) ===\n");
            Console.WriteLine("Testiranje sa različitim brojevima niti: 2, 4, 8, 16, 32\n");

            // Podesi difficulty za test (preporučeno 3-4 za brže testiranje)
            uint difficulty = 3;
            Console.WriteLine($"Difficulty: {difficulty}\n");

            var results = new List<ScalingResult>();
            long baselineTimeMs = 0;

            int[] threadCounts = { 2, 4, 8, 16, 32 };

            foreach (int numThreads in threadCounts)
            {
                Console.WriteLine($"--- Test sa {numThreads} niti ---");
                var result = TestMiningWithThreads(numThreads, difficulty);

                if (numThreads == 2)
                {
                    baselineTimeMs = result.MiningTimeMs;
                    result.Speedup = 1.0; // Baseline
                }
                else
                {
                    // Izračunaj ubrzanje, ali izbegni deljenje sa 0
                    if (result.MiningTimeMs > 0)
                    {
                        result.Speedup = baselineTimeMs > 0 
                            ? (double)baselineTimeMs / result.MiningTimeMs 
                            : 1.0;
                    }
                    else
                    {
                        // Ako je vreme 0 ms, postavi velik broj umesto beskonačnosti
                        result.Speedup = baselineTimeMs > 0 ? 1000.0 : 1.0;
                    }
                }

                results.Add(result); // Dodaj rezultat SA izračunatim Speedup-om

                Console.WriteLine($"Vreme rudarjenja: {result.MiningTimeMs} ms");
                Console.WriteLine($"Ukupno pokušaja: {result.TotalAttempts:N0}");
                if (result.FoundBlock != null)
                {
                    Console.WriteLine($"Pronađen nonce: {result.FoundBlock.Nonce:N0}");
                    Console.WriteLine($"Pronašla nit: {result.FoundByThreadId}");
                    Console.WriteLine($"Hash: {result.FoundBlock.Hash.Substring(0, Math.Min(20, result.FoundBlock.Hash.Length))}...");
                }
                string speedupDisplay = double.IsInfinity(result.Speedup) || result.Speedup >= 1000.0 
                    ? "∞x" 
                    : $"{result.Speedup:F2}x";
                Console.WriteLine($"Ubrzanje (relativno na 2 niti): {speedupDisplay}");
                Console.WriteLine();
            }

            // Ispiši rezultate u tabeli
            PrintResultsTable(results);

            // Ispiši graf pohitritev (tekstualni)
            PrintSpeedupGraph(results);
        }

        private static ScalingResult TestMiningWithThreads(int numThreads, uint difficulty)
        {
            // Kreiraj blok za rudarjenje
            var blockToMine = new Block(
                index: 1,
                data: $"Test block mined with {numThreads} threads",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: difficulty,
                nonce: 0
            );

            // Kreiraj ThreadPool
            var pool = new ThreadedMiner.MiningThreadPool(numThreads);
            var sharedState = pool.SharedState;

            // Kreiraj workers za svaku nit
            var workers = new List<ThreadedMiner.MiningWorker>();
            for (int i = 0; i < numThreads; i++)
            {
                var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(numThreads, i);
                
                // Napravi kopiju bloka za svaku nit (ali će svi deliti isti shared state)
                var blockCopy = new Block(
                    blockToMine.Index,
                    blockToMine.Data,
                    blockToMine.Timestamp,
                    blockToMine.PreviousHash,
                    blockToMine.Difficulty,
                    blockToMine.Nonce
                );

                var worker = new ThreadedMiner.BlockMiningWorker(
                    threadId: i,
                    sharedState: sharedState,
                    blockToMine: blockCopy,
                    startNonce: startNonce,
                    endNonce: endNonce
                );

                workers.Add(worker);
            }

            // Meri vreme
            var stopwatch = Stopwatch.StartNew();
            pool.StartWithWorkers(workers);
            pool.WaitAll();
            stopwatch.Stop();

            // Sakupljaj rezultate
            var result = new ScalingResult
            {
                ThreadCount = numThreads,
                MiningTimeMs = stopwatch.ElapsedMilliseconds,
                TotalAttempts = sharedState.GetTotalAttempts(),
                FoundBlock = sharedState.GetFoundBlock(),
                FoundByThreadId = sharedState.GetFoundByThreadId()
            };

            pool.Stop();
            return result;
        }

        private static void PrintResultsTable(List<ScalingResult> results)
        {
            Console.WriteLine("=== Tabela rezultata ===");
            Console.WriteLine($"{"Niti",6} {"Vreme (ms)",12} {"Pokušaji",15} {"Ubrzanje",12} {"Nonce",15}");
            Console.WriteLine(new string('-', 70));

            foreach (var result in results)
            {
                string nonceStr = result.FoundBlock != null 
                    ? $"{result.FoundBlock.Nonce:N0}" 
                    : "N/A";

                string speedupStr = double.IsInfinity(result.Speedup) || result.Speedup >= 1000.0 
                    ? "∞x" 
                    : $"{result.Speedup:F2}x";

                Console.WriteLine($"{result.ThreadCount,6} " +
                                $"{result.MiningTimeMs,12:N0} " +
                                $"{result.TotalAttempts,15:N0} " +
                                $"{speedupStr,10} " +
                                $"{nonceStr,15}");
            }
            Console.WriteLine();
        }

        private static void PrintSpeedupGraph(List<ScalingResult> results)
        {
            Console.WriteLine("=== Graf pohitritev (relativno na 2 niti) ===");
            
            if (results.Count == 0)
            {
                Console.WriteLine("Nema rezultata za prikaz.");
                return;
            }

            // Filtriraj beskonačne vrednosti za max (koristimo maksimum konačnih vrednosti)
            double maxSpeedup = results.Where(r => r.Speedup < double.MaxValue && !double.IsInfinity(r.Speedup)).Max(r => r.Speedup);
            const int barWidth = 50;

            // Ako je maxSpeedup 0 ili negativan, koristimo 1 kao default
            if (maxSpeedup <= 0)
            {
                maxSpeedup = 1.0;
            }

            foreach (var result in results)
            {
                string speedupStr;
                int barLength;
                
                // Proveri da li je ubrzanje beskonačno ili veoma veliko
                if (double.IsInfinity(result.Speedup) || result.Speedup >= 1000.0)
                {
                    speedupStr = "∞x";
                    // Za beskonačno, prikaži punu traku
                    barLength = barWidth;
                }
                else
                {
                    speedupStr = $"{result.Speedup:F2}x";
                    double normalizedSpeedup = Math.Max(0, result.Speedup / maxSpeedup);
                    barLength = Math.Max(0, Math.Min(barWidth, (int)(normalizedSpeedup * barWidth)));
                }
                
                string bar = new string('█', barLength);
                string emptyBar = new string(' ', barWidth - barLength);

                Console.WriteLine($"{result.ThreadCount,3} niti: {bar}{emptyBar} {speedupStr,6}");
            }
            Console.WriteLine();
        }

        /// <summary>
        /// Testiranje sa jednim threadom za poređenje (baseline)
        /// </summary>
        private static ScalingResult TestMiningSingleThread(uint difficulty)
        {
            var blockToMine = new Block(
                index: 1,
                data: "Test block mined with 1 thread",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "0",
                difficulty: difficulty,
                nonce: 0
            );

            string targetPrefix = new string('0', (int)difficulty);
            long attempts = 0;

            var stopwatch = Stopwatch.StartNew();

            while (true)
            {
                blockToMine.Nonce++;
                blockToMine.Hash = blockToMine.CalculateHash();
                attempts++;

                if (blockToMine.Hash.StartsWith(targetPrefix))
                {
                    break;
                }
            }

            stopwatch.Stop();

            return new ScalingResult
            {
                ThreadCount = 1,
                MiningTimeMs = stopwatch.ElapsedMilliseconds,
                TotalAttempts = attempts,
                FoundBlock = blockToMine,
                FoundByThreadId = 0,
                Speedup = 1.0
            };
        }
    }
}

