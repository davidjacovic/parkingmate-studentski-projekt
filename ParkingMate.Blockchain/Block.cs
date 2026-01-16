using System;
using System.Security.Cryptography;
using System.Text;

namespace ParkingMate.Blockchain
{
    [Serializable]
    public class Block
    {
        public uint Index { get; set; }
        public string Data { get; set; } = string.Empty;
        public long Timestamp { get; set; }
        public string PreviousHash { get; set; } = "0";
        public uint Difficulty { get; set; }
        public ulong Nonce { get; set; }
        public string Hash { get; set; } = string.Empty;

        // Obavezno za MPI serialization
        public Block() { }

        public Block(uint index, string data, long timestamp, string previousHash, uint difficulty, ulong nonce)
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
            => $"{Index}{Data}{Timestamp}{PreviousHash}{Difficulty}{Nonce}";

        public string CalculateHash()
        {
            using var sha256 = SHA256.Create();
            byte[] inputBytes = Encoding.UTF8.GetBytes(Serialize());
            byte[] hashBytes = sha256.ComputeHash(inputBytes);
            return Convert.ToHexString(hashBytes).ToLowerInvariant();
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
    }
}
