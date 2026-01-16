using System;
using System.Collections.Generic;

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
                    index: 0, // ignoriše se u AddBlock
                    data: $"Block {i}",
                    timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    previousHash: "", // ignoriše se u AddBlock
                    difficulty: 2,
                    nonce: 0
                ));
            }

            if (!bc.IsValidChain())
                throw new Exception("FAIL: Lanac mora biti validan posle dodavanja 3 bloka.");

            Console.WriteLine("OK: Lanac validan posle rudarenja.");

            // 2) Tamper: pokvari 2. blok (index 2 u listi je treći blok; uzmi index 1 da bude prvi posle genesis)
            var tampered = bc.Chain[1];
            tampered.PreviousHash = "evil";

            if (bc.IsValidChain())
                throw new Exception("FAIL: Lanac mora postati nevalidan posle tamper-a.");

            Console.WriteLine("OK: Lanac postaje nevalidan posle tamper-a.");

            // 3) Test TryReplaceChain (kumulativna težina)
            // Napravi novu validnu chain listu: uzmi fresh blockchain, napravi 1 blok sa većom difficulty
            var bc2 = new Blockchain(blockIntervalSeconds: 10, adjustmentInterval: 10, threadCount: 1);
            bc2.AddBlock(new Block(
                index: 0,
                data: "Heavier chain block",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "",
                difficulty: 4,  // veća difficulty => veća težina
                nonce: 0
            ));

            // bc je trenutno "pokvaren" (tamperovan) pa napravi novi čist bc3 da realno testira replace
            var bc3 = new Blockchain(blockIntervalSeconds: 10, adjustmentInterval: 10, threadCount: 1);
            bc3.AddBlock(new Block(0, "A", DateTimeOffset.UtcNow.ToUnixTimeSeconds(), "", 2, 0));

            bool replaced = bc3.TryReplaceChain(bc2.Chain);
            if (!replaced)
                throw new Exception("FAIL: TryReplaceChain treba da prihvati teži validan lanac.");

            Console.WriteLine("OK: TryReplaceChain prihvata teži validan lanac.");
            Console.WriteLine("=== PASS ===\n");
        }
    }
}
