using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Test klasa za testiranje detekcije CPU jezgara (Subtask 4.2.1)
    /// </summary>
    public class TestCpuDetection
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test detekcije CPU jezgara (4.2.1) ===\n");

            // Test 1: Detekcija logičkih procesora
            Console.WriteLine("Test 1: Detekcija logičkih procesora");
            int logicalCores = ThreadedMiner.GetAvailableProcessorCount();
            Console.WriteLine($"  Broj logičkih procesora: {logicalCores}");
            Console.WriteLine($"  ✓ Detekcija uspešna\n");

            // Test 2: Detekcija fizičkih jezgara
            Console.WriteLine("Test 2: Detekcija fizičkih jezgara (aproksimacija)");
            int physicalCores = ThreadedMiner.GetPhysicalProcessorCount();
            Console.WriteLine($"  Broj fizičkih jezgara (aproksimacija): {physicalCores}");
            Console.WriteLine($"  Broj logičkih procesora: {logicalCores}");
            Console.WriteLine($"  Omjer (logički/fizički): {(double)logicalCores / physicalCores:F2}");
            if (logicalCores == physicalCores * 2)
            {
                Console.WriteLine($"  ✓ Detektovan hiperthreading (HT)");
            }
            else if (logicalCores == physicalCores)
            {
                Console.WriteLine($"  ✓ Nema hiperthreading-a ili je broj već fizički");
            }
            Console.WriteLine();

            // Test 3: Optimalan broj niti (sa rezervisanim jezgrom)
            Console.WriteLine("Test 3: Optimalan broj niti (sa rezervisanim jezgrom)");
            int optimalThreads = ThreadedMiner.GetOptimalThreadCount(usePhysicalCores: false, reserveCores: 1);
            Console.WriteLine($"  Optimalan broj niti (logički, sa rezervisanim jezgrom): {optimalThreads}");
            Console.WriteLine($"  Koristi logičke procesore: {logicalCores}");
            Console.WriteLine($"  Rezervisano jezgara: 1");
            Console.WriteLine($"  Dostupno za rudarjenje: {optimalThreads}");
            Console.WriteLine();

            // Test 4: Optimalan broj niti (bez rezervisanja)
            Console.WriteLine("Test 4: Optimalan broj niti (maksimalna performansa)");
            int maxPerformanceThreads = ThreadedMiner.GetOptimalThreadCountMaxPerformance(usePhysicalCores: false);
            Console.WriteLine($"  Optimalan broj niti (logički, maksimalna performansa): {maxPerformanceThreads}");
            Console.WriteLine($"  Koristi sve dostupne logičke procesore: {logicalCores}");
            Console.WriteLine();

            // Test 5: Optimalan broj niti (fizički procesori)
            Console.WriteLine("Test 5: Optimalan broj niti (fizički procesori)");
            int physicalOptimal = ThreadedMiner.GetOptimalThreadCount(usePhysicalCores: true, reserveCores: 1);
            Console.WriteLine($"  Optimalan broj niti (fizički, sa rezervisanim jezgrom): {physicalOptimal}");
            Console.WriteLine($"  Koristi fizičke jezgre: {physicalCores}");
            Console.WriteLine($"  Rezervisano jezgara: 1");
            Console.WriteLine($"  Dostupno za rudarjenje: {physicalOptimal}");
            Console.WriteLine();

            // Test 6: Različite strategije
            Console.WriteLine("Test 6: Poređenje različitih strategija");
            Console.WriteLine($"  Strategija 1 - Svi logički procesori: {logicalCores} niti");
            Console.WriteLine($"  Strategija 2 - Logički - 1 (rezervisano): {Math.Max(1, logicalCores - 1)} niti");
            Console.WriteLine($"  Strategija 3 - Svi fizički procesori: {physicalCores} niti");
            Console.WriteLine($"  Strategija 4 - Fizički - 1 (rezervisano): {Math.Max(1, physicalCores - 1)} niti");
            Console.WriteLine();

            // Test 7: Validacija
            Console.WriteLine("Test 7: Validacija rezultata");
            bool isValid = true;
            
            if (logicalCores < 1)
            {
                Console.WriteLine("  ✗ Greška: Broj logičkih procesora mora biti >= 1");
                isValid = false;
            }
            
            if (physicalCores < 1)
            {
                Console.WriteLine("  ✗ Greška: Broj fizičkih jezgara mora biti >= 1");
                isValid = false;
            }
            
            if (physicalCores > logicalCores)
            {
                Console.WriteLine("  ✗ Greška: Broj fizičkih jezgara ne može biti veći od logičkih");
                isValid = false;
            }
            
            if (optimalThreads < 1 || optimalThreads > logicalCores)
            {
                Console.WriteLine($"  ✗ Greška: Optimalan broj niti ({optimalThreads}) je van opsega [1, {logicalCores}]");
                isValid = false;
            }
            
            if (isValid)
            {
                Console.WriteLine("  ✓ Svi rezultati su validni");
            }
            Console.WriteLine();

            // Sažetak
            Console.WriteLine("=== Sažetak ===");
            Console.WriteLine($"Dostupno logičkih procesora: {logicalCores}");
            Console.WriteLine($"Dostupno fizičkih jezgara (aproksimacija): {physicalCores}");
            Console.WriteLine($"Preporučen broj niti (sa rezervisanim jezgrom): {optimalThreads}");
            Console.WriteLine($"Preporučen broj niti (maksimalna performansa): {maxPerformanceThreads}");
            Console.WriteLine();
            
            if (logicalCores >= 4)
            {
                Console.WriteLine("💡 Preporuka: Za optimalnu performansu, koristi sve dostupne logičke procesore.");
                Console.WriteLine("   Za testiranje skaliranja, možeš koristiti manje niti (2, 4, 8, 16, 32).");
            }
            else
            {
                Console.WriteLine("💡 Preporuka: Sistem ima mali broj procesora. Koristi sve dostupne za maksimalnu performansu.");
            }
        }
    }
}

