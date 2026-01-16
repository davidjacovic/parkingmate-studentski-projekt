using System;

namespace ParkingMate.Blockchain
{
    public static class TestMiningCreatesValidBlock
    {
        public static void RunTest()
        {
            Console.WriteLine("=== TestMiningCreatesValidBlock ===");

            int threads = Math.Min(4, ThreadedMiner.GetAvailableProcessorCount());
            var bc = new Blockchain(blockIntervalSeconds: 10, adjustmentInterval: 10, threadCount: threads);

            var prev = bc.GetLatestBlock();

            // Candidate: AddBlock će ignorisati index/prevhash i postaviti ih sam
            var candidate = new Block(
                index: 0,
                data: "Mined block test",
                timestamp: DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                previousHash: "",
                difficulty: 3, // spusti na 2 ako je presporo
                nonce: 0
            );

            int before = bc.Chain.Count;
            bc.AddBlock(candidate);
            int after = bc.Chain.Count;

            if (after != before + 1)
                throw new Exception("FAIL: Chain count se nije povećao za 1 posle AddBlock.");

            var latest = bc.GetLatestBlock();

            // 1) PoW prefix
            string prefix = new string('0', (int)latest.Difficulty);
            if (latest.Hash == null || !latest.Hash.StartsWith(prefix))
                throw new Exception("FAIL: Hash ne zadovoljava PoW prefix (difficulty).");

            // 2) Stored hash == CalculateHash
            var calc = latest.CalculateHash();
            if (calc != latest.Hash)
                throw new Exception("FAIL: latest.Hash != latest.CalculateHash().");

            // 3) IsValidNewBlock prema prethodnom
            if (!bc.IsValidNewBlock(latest, prev))
                throw new Exception("FAIL: IsValidNewBlock odbacuje block koji je AddBlock upravo dodao.");

            // 4) IsValidChain
            if (!bc.IsValidChain())
                throw new Exception("FAIL: IsValidChain je false nakon dodavanja bloka.");

            Console.WriteLine("OK: AddBlock(mining) kreira validan blok i validan lanac.");
            Console.WriteLine($"  Nonce: {latest.Nonce:N0}");
            Console.WriteLine($"  Hash:  {latest.Hash.Substring(0, Math.Min(24, latest.Hash.Length))}...");
            Console.WriteLine("=== PASS ===\n");
        }
    }
}
