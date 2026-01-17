using Microsoft.AspNetCore.Mvc;
using ParkingMate.Blockchain;
using ParkingMate.Blockchain.Infrastructure;
using ParkingMate.BlockchainService.Dtos;
using System.Numerics;
using System.Linq;

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

        /// <summary>
        /// Verifikuje da li je blok sa određenim hash-om validan i prisutan u lancu.
        /// GET /api/blockchain/verify/{hash}
        /// </summary>
        [HttpGet("verify/{hash}")]
        public ActionResult<VerifyBlockResponseDto> VerifyBlock(string hash)
        {
            try
            {
                if (string.IsNullOrWhiteSpace(hash))
                {
                    return BadRequest(new VerifyBlockResponseDto
                    {
                        Verified = false,
                        Found = false,
                        IntegrityValid = false,
                        ChainValid = false,
                        Message = "Hash is required"
                    });
                }

                // Direktno pretraživanje lanca (bez reflection metoda)
                var chain = _state.Chain.Chain;
                var block = chain.FirstOrDefault(b => b.Hash.Equals(hash, StringComparison.OrdinalIgnoreCase));
                
                if (block == null)
                {
                    return Ok(new VerifyBlockResponseDto
                    {
                        Verified = false,
                        Found = false,
                        IntegrityValid = false,
                        ChainValid = false,
                        Message = $"Block with hash '{hash}' not found in blockchain"
                    });
                }

                // Verifikuj integritet bloka direktno
                bool integrityValid = VerifyBlockIntegrityDirect(block);

                // Verifikuj da li je blok validan u kontekstu lanca
                int index = -1;
                for (int i = 0; i < chain.Count; i++)
                {
                    if (chain[i].Hash.Equals(block.Hash, StringComparison.OrdinalIgnoreCase))
                    {
                        index = i;
                        break;
                    }
                }
                
                bool chainValid = false;
                
                if (index >= 0)
                {
                    if (index == 0)
                    {
                        // Genesis blok - samo proveri hash
                        chainValid = block.Hash == block.CalculateHash();
                    }
                    else
                    {
                        // Proveri validaciju u kontekstu prethodnog bloka
                        var previousBlock = chain[index - 1];
                        chainValid = block.Index == previousBlock.Index + 1 
                            && block.PreviousHash == previousBlock.Hash
                            && integrityValid;
                    }
                }

                bool verified = integrityValid && chainValid;

                return Ok(new VerifyBlockResponseDto
                {
                    Verified = verified,
                    Found = true,
                    IntegrityValid = integrityValid,
                    ChainValid = chainValid,
                    Message = verified 
                        ? $"Block with hash '{hash}' is verified and valid in chain" 
                        : $"Block with hash '{hash}' found but verification failed",
                    Block = ToDto(block)
                });
            }
            catch (Exception ex)
            {
                return StatusCode(500, new VerifyBlockResponseDto
                {
                    Verified = false,
                    Found = false,
                    IntegrityValid = false,
                    ChainValid = false,
                    Message = $"Error verifying block: {ex.Message}"
                });
            }
        }

        /// <summary>
        /// Pronalazi blokove koji sadrže određene podatke.
        /// GET /api/blockchain/search?data={data}
        /// </summary>
        [HttpGet("search")]
        public ActionResult<IEnumerable<BlockDto>> SearchBlocks([FromQuery] string data)
        {
            if (string.IsNullOrWhiteSpace(data))
            {
                return BadRequest(new ErrorResponseDto
                {
                    ErrorCode = "VALIDATION_ERROR",
                    Message = "Search data parameter is required"
                });
            }

            // Koristimo reflection za pristup metodi
            var chainType = _state.Chain.GetType();
            var findMethod = chainType.GetMethod("FindBlocksByData", new[] { typeof(string) });

            if (findMethod == null)
            {
                return StatusCode(500, new ErrorResponseDto
                {
                    ErrorCode = "METHOD_NOT_AVAILABLE",
                    Message = "FindBlocksByData method not available"
                });
            }

            var blocks = findMethod.Invoke(_state.Chain, new object[] { data }) as System.Collections.Generic.List<Block>;
            
            if (blocks == null)
                return Ok(new List<BlockDto>());

            var dtos = blocks.Select(ToDto).ToList();
            return Ok(dtos);
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

        private static bool VerifyBlockIntegrityDirect(Block block)
        {
            if (block == null)
                return false;

            // Proveri da li hash odgovara izračunatom hash-u
            string calculatedHash = block.CalculateHash();
            if (block.Hash != calculatedHash)
                return false;

            // Proveri da li hash ispunjava difficulty zahtev
            string prefix = new string('0', (int)block.Difficulty);
            if (!block.Hash.StartsWith(prefix))
                return false;

            return true;
        }
    }
}
