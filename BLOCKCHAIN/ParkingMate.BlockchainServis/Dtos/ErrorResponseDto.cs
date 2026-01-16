namespace ParkingMate.BlockchainService.Dtos
{
    public class ErrorResponseDto
    {
        public string ErrorCode { get; set; } = "INTERNAL_ERROR";
        public string Message { get; set; } = "Unexpected error";
        public List<string>? Details { get; set; }
    }
}
