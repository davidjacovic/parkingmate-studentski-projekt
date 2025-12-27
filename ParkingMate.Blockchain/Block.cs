using System;
using System.Text;
using System.Security.Cryptography;


namespace ParkingMate.Blockchain
{
    public class Block
    {
        public uint Index { get; }
        public string Data { get; }
        public long Timestamp { get; }
        public string PreviousHash { get; }
        public uint Difficulty { get; }
        public ulong Nonce { get; set; }
        public string Hash { get; set; }

        public Block(
            uint index,
            string data,
            long timestamp,
            string previousHash,
            uint difficulty,
            ulong nonce)
        {
            Index = index;
            Data = data;
            Timestamp = timestamp;
            PreviousHash = previousHash;
            Difficulty = difficulty;
            Nonce = nonce;
            Hash = string.Empty;
        }

        public string Serialize()
        {
            return $"{Index}{Data}{Timestamp}{PreviousHash}{Difficulty}{Nonce}";
        }

        public override string ToString()
        {
            var sb = new StringBuilder();
            sb.AppendLine("Block {");
            sb.AppendLine($"  index: {Index}");
            sb.AppendLine($"  data: {Data}");
            sb.AppendLine($"  timestamp: {Timestamp}");
            sb.AppendLine($"  previousHash: {PreviousHash}");
            sb.AppendLine($"  difficulty: {Difficulty}");
            sb.AppendLine($"  nonce: {Nonce}");
            sb.AppendLine($"  hash: {Hash}");
            sb.AppendLine("}");
            return sb.ToString();
        }

        public string CalculateHash()
        {
            using var sha256 = SHA256.Create();
            byte[] inputBytes = Encoding.UTF8.GetBytes(Serialize());
            byte[] hashBytes = sha256.ComputeHash(inputBytes);

            return Convert.ToHexString(hashBytes).ToLower();
        }

    }
}
