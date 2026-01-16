namespace ParkingMate.BlockchainService.Dtos
{
    public class BlockDto
    {
        public uint Index { get; set; }
        public string Data { get; set; } = string.Empty;
        public long Timestamp { get; set; }
        public string PreviousHash { get; set; } = "0";
        public uint Difficulty { get; set; }
        public ulong Nonce { get; set; }
        public string Hash { get; set; } = string.Empty;
    }
}
