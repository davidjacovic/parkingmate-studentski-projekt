using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje dizajna podele nonce prostora (Subtask 4.1.1)
    /// </summary>
    public class TestNonceRangeDivision
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test podele nonce prostora po nitima ===\n");

            // Test sa 4 niti
            int numThreads = 4;
            Console.WriteLine($"Test sa {numThreads} niti:\n");

            for (int i = 0; i < numThreads; i++)
            {
                var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(numThreads, i);
                Console.WriteLine($"Thread {i}: [{startNonce:N0}, {endNonce:N0}]");
                Console.WriteLine($"  Opseg: {endNonce - startNonce:N0} nonce vrednosti\n");
            }

            // Test sa 8 niti
            numThreads = 8;
            Console.WriteLine($"\nTest sa {numThreads} niti:\n");

            for (int i = 0; i < numThreads; i++)
            {
                var (startNonce, endNonce) = ThreadedMiner.CalculateNonceRange(numThreads, i);
                Console.WriteLine($"Thread {i}: [{startNonce:N0}, {endNonce:N0}]");
            }

            // Provera da li se opsezi ne preklapaju
            Console.WriteLine("\n=== Provera preklapanja opsega ===");
            bool hasOverlap = false;
            for (int threads = 2; threads <= 16; threads *= 2)
            {
                bool overlap = CheckOverlap(threads);
                Console.WriteLine($"{threads} niti: {(overlap ? "PREKLAPANJE!" : "OK - bez preklapanja")}");
                if (overlap) hasOverlap = true;
            }

            if (!hasOverlap)
            {
                Console.WriteLine("\n✓ Svi opsezi su ispravno podešeni - nema preklapanja!");
            }
        }

        private static bool CheckOverlap(int numThreads)
        {
            ulong? prevEnd = null;
            for (int i = 0; i < numThreads; i++)
            {
                var (start, end) = ThreadedMiner.CalculateNonceRange(numThreads, i);
                if (prevEnd.HasValue && start < prevEnd.Value)
                {
                    return true; // Preklapanje
                }
                prevEnd = end;
            }
            return false;
        }
    }
}


