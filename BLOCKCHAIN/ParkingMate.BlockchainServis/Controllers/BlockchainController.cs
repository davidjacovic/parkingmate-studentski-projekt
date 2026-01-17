using Microsoft.AspNetCore.Mvc;
using ParkingMate.Blockchain;
using ParkingMate.Blockchain.Infrastructure;
using ParkingMate.BlockchainService.Dtos;
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

            const int MaxDataLength = 1024;


            var errors = new List<string>();
            if (request == null)
            {
                errors.Add("request body is required");
            }
            else
            {
                if (string.IsNullOrWhiteSpace(request.Data))
                    errors.Add("data is required");

                if (request.Data.Length > MaxDataLength)
                    errors.Add($"data must be <= {MaxDataLength} characters");

                if (request.Timestamp <= 0)
                    errors.Add("timestamp must be unix seconds");
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
                // 1) Napravi "newBlock" samo sa Data (ostalo Blockchain.AddBlock popunjava)
                // request.Data je već validiran kao non-null i non-empty gore
                var input = new Block(
                    index: 0,
                    data: request.Data!,
                    timestamp: 0,
                    previousHash: "",
                    difficulty: 0,
                    nonce: 0
                );

                // 2) Rudari + append (tvoja postojeća logika)
                _state.Chain.AddBlock(input);

                // 3) Sačuvaj blockchain u storage nakon dodavanja novog bloka
                _state.SaveChain();

                // 4) Vrati poslednji blok kao response (201)
                var mined = _state.Chain.GetLatestBlock();
                return Created("", ToDto(mined));
            }
            catch (Exception ex)
            {
                return StatusCode(500, new ErrorResponseDto
                {
                    ErrorCode = "INTERNAL_ERROR",
                    Message = "Mining failed due to server error",
                    Details = new List<string> { ex.Message }
                });
            }
            finally
            {
                // Oslobodi lock da bi sledeći zahtev mogao da prođe
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
