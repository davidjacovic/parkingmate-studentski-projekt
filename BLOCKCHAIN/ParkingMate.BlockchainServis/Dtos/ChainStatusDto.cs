namespace ParkingMate.BlockchainService.Dtos
{
    public class ChainStatusDto
    {
        public int Length { get; set; }
        public uint LatestIndex { get; set; }
        public string LatestHash { get; set; } = string.Empty;
        public string CumulativeWeight { get; set; } = "0";
        public List<BlockDto> Chain { get; set; } = new();
    }
}
