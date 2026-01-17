namespace ParkingMate.BlockchainService.Dtos
{
    public class VerifyBlockResponseDto
    {
        public bool Verified { get; set; }
        public bool Found { get; set; }
        public bool IntegrityValid { get; set; }
        public bool ChainValid { get; set; }
        public string Message { get; set; } = string.Empty;
        public BlockDto? Block { get; set; }
    }
}

