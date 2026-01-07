using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Testovi za validaciju timestamp-a blokova (TASK 2.3).
    /// </summary>
    public static class TestTimestampValidation
    {
        public static void RunTest()
        {
            Console.WriteLine("=== Test validacije timestamp-a (TASK 2.3) ===");
            Console.WriteLine();

            int passed = 0;
            int failed = 0;

            // Test 1: Validan timestamp (normalan slučaj)
            try
            {
                TestValidTimestamp(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 1: Validan timestamp - Greška: {ex.Message}");
                failed++;
            }

            // Test 2: Timestamp u budućnosti (previše)
            try
            {
                TestFutureTimestamp(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 2: Timestamp u budućnosti - Greška: {ex.Message}");
                failed++;
            }

            // Test 3: Timestamp manji ili jednak prethodnom bloku
            try
            {
                TestTimestampNotGreaterThanPrevious(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 3: Timestamp manji od prethodnog - Greška: {ex.Message}");
                failed++;
            }

            // Test 4: Timestamp previše u prošlosti
            try
            {
                TestTimestampTooFarInPast(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 4: Timestamp previše u prošlosti - Greška: {ex.Message}");
                failed++;
            }

            // Test 5: Genesis blok timestamp validacija
            try
            {
                TestGenesisTimestamp(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 5: Genesis blok timestamp - Greška: {ex.Message}");
                failed++;
            }

            // Test 6: Null argumenti
            try
            {
                TestNullArguments(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 6: Null argumenti - Greška: {ex.Message}");
                failed++;
            }

            // Test 7: Integracija sa Blockchain.IsValidNewBlock
            try
            {
                TestIntegrationWithBlockchain(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 7: Integracija sa Blockchain - Greška: {ex.Message}");
                failed++;
            }

            // Test 8: Custom parametri za validaciju
            try
            {
                TestCustomParameters(ref passed, ref failed);
            }
            catch (Exception ex)
            {
                Console.WriteLine($"✗ Test 8: Custom parametri - Greška: {ex.Message}");
                failed++;
            }

            Console.WriteLine();
            Console.WriteLine($"=== Rezultat: {passed} prošlo, {failed} palo ===");
            if (failed == 0)
            {
                Console.WriteLine("✓ Testovi za Subtask 2.3 (Validacija timestamp-a) su prošli!");
            }
            else
            {
                Console.WriteLine($"✗ Neki testovi su pali ({failed} od {passed + failed})");
            }
        }

        private static void TestValidTimestamp(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 1: Validan timestamp (normalan slučaj)");
            
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long previousTimestamp = currentTime - 100; // Prethodni blok je 100 sekundi u prošlosti
            long currentTimestamp = currentTime - 50; // Trenutni blok je 50 sekundi u prošlosti

            var previousBlock = new Block(0, "Previous", previousTimestamp, "0", 1, 0);
            var currentBlock = new Block(1, "Current", currentTimestamp, previousBlock.Hash, 1, 0);

            bool isValid = TimestampValidator.IsValidTimestamp(currentBlock, previousBlock);
            
            if (isValid)
            {
                Console.WriteLine("✓ Validan timestamp je prihvaćen");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Validan timestamp je odbačen");
                failed++;
            }
        }

        private static void TestFutureTimestamp(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 2: Timestamp u budućnosti (previše)");
            
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long previousTimestamp = currentTime - 100;
            long futureTimestamp = currentTime + TimestampValidator.MaxFutureTimeSeconds + 100; // Previše u budućnosti

            var previousBlock = new Block(0, "Previous", previousTimestamp, "0", 1, 0);
            var currentBlock = new Block(1, "Current", futureTimestamp, previousBlock.Hash, 1, 0);

            bool isValid = TimestampValidator.IsValidTimestamp(currentBlock, previousBlock);
            
            if (!isValid)
            {
                Console.WriteLine("✓ Timestamp u budućnosti je odbačen");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Timestamp u budućnosti je prihvaćen");
                failed++;
            }
        }

        private static void TestTimestampNotGreaterThanPrevious(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 3: Timestamp manji ili jednak prethodnom bloku");
            
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long previousTimestamp = currentTime - 100;
            long currentTimestamp = previousTimestamp - 10; // Manji od prethodnog

            var previousBlock = new Block(0, "Previous", previousTimestamp, "0", 1, 0);
            var currentBlock = new Block(1, "Current", currentTimestamp, previousBlock.Hash, 1, 0);

            bool isValid = TimestampValidator.IsValidTimestamp(currentBlock, previousBlock);
            
            if (!isValid)
            {
                Console.WriteLine("✓ Timestamp manji od prethodnog je odbačen");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Timestamp manji od prethodnog je prihvaćen");
                failed++;
            }

            // Test sa jednakim timestamp-om
            currentTimestamp = previousTimestamp; // Isti kao prethodni
            var currentBlock2 = new Block(1, "Current", currentTimestamp, previousBlock.Hash, 1, 0);
            bool isValid2 = TimestampValidator.IsValidTimestamp(currentBlock2, previousBlock);
            
            if (!isValid2)
            {
                Console.WriteLine("✓ Timestamp jednak prethodnom je odbačen");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Timestamp jednak prethodnom je prihvaćen");
                failed++;
            }
        }

        private static void TestTimestampTooFarInPast(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 4: Timestamp previše u prošlosti");
            
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long previousTimestamp = currentTime - 100;
            long currentTimestamp = previousTimestamp - TimestampValidator.MaxPastTimeSeconds - 100; // Previše u prošlosti

            var previousBlock = new Block(0, "Previous", previousTimestamp, "0", 1, 0);
            var currentBlock = new Block(1, "Current", currentTimestamp, previousBlock.Hash, 1, 0);

            bool isValid = TimestampValidator.IsValidTimestamp(currentBlock, previousBlock);
            
            if (!isValid)
            {
                Console.WriteLine("✓ Timestamp previše u prošlosti je odbačen");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Timestamp previše u prošlosti je prihvaćen");
                failed++;
            }
        }

        private static void TestGenesisTimestamp(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 5: Genesis blok timestamp validacija");
            
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            
            // Genesis blok u prošlosti (validan)
            long pastTimestamp = currentTime - 1000;
            var genesisPast = new Block(0, "Genesis", pastTimestamp, "0", 1, 0);
            bool isValidPast = TimestampValidator.IsValidGenesisTimestamp(genesisPast);
            
            if (isValidPast)
            {
                Console.WriteLine("✓ Genesis blok u prošlosti je validan");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Genesis blok u prošlosti je odbačen");
                failed++;
            }

            // Genesis blok u budućnosti (previše - nevalidan)
            long futureTimestamp = currentTime + TimestampValidator.MaxFutureTimeSeconds + 100;
            var genesisFuture = new Block(0, "Genesis", futureTimestamp, "0", 1, 0);
            bool isValidFuture = TimestampValidator.IsValidGenesisTimestamp(genesisFuture);
            
            if (!isValidFuture)
            {
                Console.WriteLine("✓ Genesis blok previše u budućnosti je odbačen");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Genesis blok previše u budućnosti je prihvaćen");
                failed++;
            }
        }

        private static void TestNullArguments(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 6: Null argumenti");
            
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            var block = new Block(0, "Test", currentTime, "0", 1, 0);

            // Test null currentBlock
            try
            {
                TimestampValidator.IsValidTimestamp(null!, block);
                Console.WriteLine("✗ Null currentBlock nije bacio izuzetak");
                failed++;
            }
            catch (ArgumentNullException)
            {
                Console.WriteLine("✓ Null currentBlock baca ArgumentNullException");
                passed++;
            }

            // Test null previousBlock
            try
            {
                TimestampValidator.IsValidTimestamp(block, null!);
                Console.WriteLine("✗ Null previousBlock nije bacio izuzetak");
                failed++;
            }
            catch (ArgumentNullException)
            {
                Console.WriteLine("✓ Null previousBlock baca ArgumentNullException");
                passed++;
            }

            // Test null genesisBlock
            try
            {
                TimestampValidator.IsValidGenesisTimestamp(null!);
                Console.WriteLine("✗ Null genesisBlock nije bacio izuzetak");
                failed++;
            }
            catch (ArgumentNullException)
            {
                Console.WriteLine("✓ Null genesisBlock baca ArgumentNullException");
                passed++;
            }
        }

        private static void TestIntegrationWithBlockchain(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 7: Integracija sa Blockchain.IsValidNewBlock");
            
            var blockchain = new Blockchain();
            var genesis = blockchain.GetLatestBlock();

            // Kreiraj blok sa validnim timestamp-om
            // Timestamp mora biti veći od genesis timestamp-a, ali ne previše u budućnosti
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long validTimestamp = Math.Max(genesis.Timestamp + 1, currentTime - 10); // Bar 1 sekunda posle genesis-a, ali ne previše u prošlosti
            
            var validBlock = new Block(1, "Valid Block", validTimestamp, genesis.Hash, 1, 0);
            validBlock.Hash = validBlock.CalculateHash();
            
            // Pošto hash ne zadovoljava difficulty, moramo da ga "minujemo" ili da koristimo nisku difficulty
            // Za test, koristimo blok sa difficulty=1 i pronađemo validan hash
            while (!validBlock.Hash.StartsWith("0"))
            {
                validBlock.Nonce++;
                validBlock.Hash = validBlock.CalculateHash();
            }

            bool isValid = blockchain.IsValidNewBlock(validBlock, genesis);
            
            if (isValid)
            {
                Console.WriteLine("✓ Blockchain.IsValidNewBlock prihvata blok sa validnim timestamp-om");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Blockchain.IsValidNewBlock odbacuje blok sa validnim timestamp-om");
                Console.WriteLine($"  Genesis timestamp: {genesis.Timestamp}, Block timestamp: {validTimestamp}, Current time: {currentTime}");
                failed++;
            }

            // Kreiraj blok sa nevalidnim timestamp-om (u budućnosti)
            long futureTimestamp = currentTime + TimestampValidator.MaxFutureTimeSeconds + 100;
            var invalidBlock = new Block(1, "Invalid Block", futureTimestamp, genesis.Hash, 1, 0);
            invalidBlock.Hash = invalidBlock.CalculateHash();
            
            while (!invalidBlock.Hash.StartsWith("0"))
            {
                invalidBlock.Nonce++;
                invalidBlock.Hash = invalidBlock.CalculateHash();
            }

            bool isInvalid = blockchain.IsValidNewBlock(invalidBlock, genesis);
            
            if (!isInvalid)
            {
                Console.WriteLine("✓ Blockchain.IsValidNewBlock odbacuje blok sa nevalidnim timestamp-om");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Blockchain.IsValidNewBlock prihvata blok sa nevalidnim timestamp-om");
                failed++;
            }
        }

        private static void TestCustomParameters(ref int passed, ref int failed)
        {
            Console.WriteLine("Test 8: Custom parametri za validaciju");
            
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long previousTimestamp = currentTime - 100;
            
            // Test sa custom maxFutureTimeSeconds (većim)
            long futureTimestamp = currentTime + 100; // 100 sekundi u budućnosti
            var previousBlock = new Block(0, "Previous", previousTimestamp, "0", 1, 0);
            var currentBlock = new Block(1, "Current", futureTimestamp, previousBlock.Hash, 1, 0);

            // Sa default parametrima (MaxFutureTimeSeconds = 7200), ovo bi trebalo da bude validno
            // Ali sa custom parametrima (maxFutureTimeSeconds = 50), ovo bi trebalo da bude nevalidno
            bool isValidWithCustom = TimestampValidator.IsValidTimestamp(
                currentBlock, 
                previousBlock, 
                maxFutureTimeSeconds: 50, 
                maxPastTimeSeconds: 7200
            );
            
            if (!isValidWithCustom)
            {
                Console.WriteLine("✓ Custom maxFutureTimeSeconds radi ispravno (odbacuje blok u budućnosti)");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Custom maxFutureTimeSeconds ne radi ispravno");
                failed++;
            }

            // Test sa custom maxPastTimeSeconds
            long pastTimestamp = previousTimestamp - 100; // 100 sekundi u prošlosti od prethodnog
            var pastBlock = new Block(1, "Past", pastTimestamp, previousBlock.Hash, 1, 0);
            
            bool isValidWithCustomPast = TimestampValidator.IsValidTimestamp(
                pastBlock,
                previousBlock,
                maxFutureTimeSeconds: 7200,
                maxPastTimeSeconds: 50 // Samo 50 sekundi dozvoljeno u prošlosti
            );
            
            if (!isValidWithCustomPast)
            {
                Console.WriteLine("✓ Custom maxPastTimeSeconds radi ispravno (odbacuje blok previše u prošlosti)");
                passed++;
            }
            else
            {
                Console.WriteLine("✗ Custom maxPastTimeSeconds ne radi ispravno");
                failed++;
            }

            // Test sa negativnim parametrima (treba da baci izuzetak)
            try
            {
                TimestampValidator.IsValidTimestamp(
                    currentBlock,
                    previousBlock,
                    maxFutureTimeSeconds: -1,
                    maxPastTimeSeconds: 7200
                );
                Console.WriteLine("✗ Negativni maxFutureTimeSeconds nije bacio izuzetak");
                failed++;
            }
            catch (ArgumentException)
            {
                Console.WriteLine("✓ Negativni maxFutureTimeSeconds baca ArgumentException");
                passed++;
            }
        }
    }
}

