using System;

namespace ParkingMate.Blockchain
{
    public static class TestBlockAndChainValidation
    {
        public static void RunTest()
        {
            Console.WriteLine("=== TestBlockAndChainValidation ===");

            // 1) Napravi lanac i dodaj par blokova (AddBlock rudari)
            var bc = new Blockchain(blockIntervalSeconds: 10, adjustmentInterval: 10, threadCount: 1);

            for (int i = 1; i <= 3; i++)
            {
                bc.AddBlock(new Block(
                    index: 0,
                    data: $"Block {i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "",
                    difficulty: 2,
                    nonce: 0
                ));
            }

            if (!bc.IsValidChain())
                throw new Exception("FAIL: Lanac mora biti validan posle dodavanja 3 bloka.");

            Console.WriteLine("OK: Lanac validan posle rudarenja.");

            // 2) Tamper
            var tampered = bc.Chain[1];
            tampered.PreviousHash = "evil";

            if (bc.IsValidChain())
                throw new Exception("FAIL: Lanac mora postati nevalidan posle tamper-a.");

            Console.WriteLine("OK: Lanac postaje nevalidan posle tamper-a.");

            // 3) Test TryReplaceChain (kumulativna težina)
            // Pošto AddBlock ignoriše prosleđenu difficulty, težinu pravimo tako što kandidat ima VIŠE BLOKOVA.
            var candidate = new Blockchain(blockIntervalSeconds: 10, adjustmentInterval: 10, threadCount: 1);

            // kandidat: dodaj više blokova
            for (int i = 1; i <= 5; i++)
            {
                candidate.AddBlock(new Block(
                    index: 0,
                    data: $"Candidate {i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "",
                    difficulty: 1,
                    nonce: 0
                ));
            }

            // bc3: manji lanac
            var bc3 = new Blockchain(blockIntervalSeconds: 10, adjustmentInterval: 10, threadCount: 1);
            bc3.AddBlock(new Block(0, "A", DateTimeOffset.UtcNow.ToUnixTimeSeconds(), "", 1, 0));

            bool replaced = bc3.TryReplaceChain(candidate.Chain);
            if (!replaced)
                throw new Exception("FAIL: TryReplaceChain treba da prihvati teži (kumulativno) validan lanac.");

            Console.WriteLine("OK: TryReplaceChain prihvata teži validan lanac.");
            Console.WriteLine("=== PASS ===\n");
        }
    }
}
