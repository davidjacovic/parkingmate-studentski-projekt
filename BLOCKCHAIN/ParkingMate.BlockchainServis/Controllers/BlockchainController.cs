using Microsoft.AspNetCore.Mvc;
using ParkingMate.Blockchain;
using ParkingMate.Blockchain.Infrastructure;
using ParkingMate.BlockchainService.Dtos;
using ParkingMate.Blockchain.Infrastructure;
using System.Numerics;

namespace ParkingMate.BlockchainService.Controllers
{
    [ApiController]
    [Route("api/[controller]")]

    public class BlockchainController : ControllerBase
    {
        private readonly BlockchainState _state;
        private readonly MiningGate _gate;

        public BlockchainController(BlockchainState state, MiningGate gate)
        {
            _state = state;
            _gate = gate;
        }

        [HttpGet]
        public ActionResult<ChainStatusDto> GetChain()
        {
            var chain = _state.Chain.Chain;

            var dto = new ChainStatusDto
            {
                Length = chain.Count,
                LatestIndex = chain[^1].Index,
                LatestHash = chain[^1].Hash,
                CumulativeWeight = CumulativeWeight.CalculateChainWeight(chain).ToString(),
                Chain = chain.Select(ToDto).ToList()
            };

            return Ok(dto);
        }

        [HttpGet("validate")]
        public ActionResult<ValidateResponseDto> Validate()
        {
            bool valid = _state.Chain.IsValidChain();
            return Ok(new ValidateResponseDto { Valid = valid });
        }

        [HttpPost("mine")]
        public async Task<IActionResult> Mine([FromBody] MineRequestDto request)
        {
            // Task B: endpoint postoji + basic validacija + concurrency gate
            // Task C: ovde ide stvarni mining (multithread / mpi ne ovde)

            var errors = new List<string>();
            if (request == null)
            {
                errors.Add("request body is required");
            }
            else
            {
                if (string.IsNullOrWhiteSpace(request.Data)) errors.Add("data is required");
                if (request.Timestamp <= 0) errors.Add("timestamp must be unix seconds");
            }

            if (errors.Count > 0)
            {
                return BadRequest(new ErrorResponseDto
                {
                    ErrorCode = "VALIDATION_ERROR",
                    Message = "Invalid request body",
                    Details = errors
                });
            }

            bool entered = await _gate.TryEnterAsync(TimeSpan.FromMilliseconds(1));
            if (!entered)
            {
                return Conflict(new ErrorResponseDto
                {
                    ErrorCode = "MINING_IN_PROGRESS",
                    Message = "Mining operation already in progress"
                });
            }

            try
            {
                // Stub za Task B
                return StatusCode(501, new ErrorResponseDto
                {
                    ErrorCode = "NOT_IMPLEMENTED",
                    Message = "Mining is implemented in Task C. Endpoint wiring is done."
                });
            }
            finally
            {
                _gate.Exit();
            }
        }

        private static BlockDto ToDto(Block b) => new BlockDto
        {
            Index = b.Index,
            Data = b.Data,
            Timestamp = b.Timestamp,
            PreviousHash = b.PreviousHash,
            Difficulty = b.Difficulty,
            Nonce = b.Nonce,
            Hash = b.Hash
        };
    }
}
