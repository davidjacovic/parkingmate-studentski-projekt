using ParkingMate.Blockchain;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;

namespace ParkingMate.Blockchain.Infrastructure
{
    /// <summary>
    /// Perzistencija blockchain lanca u JSON fajl.
    /// Čuva blockchain lanac na disk i učitava ga pri pokretanju servisa.
    /// </summary>
    public class BlockchainStorage
    {
        private readonly string _storageFilePath;
        private readonly JsonSerializerOptions _jsonOptions;

        public BlockchainStorage(string? storageFilePath = null)
        {
            // Default putanja: blockchain_data.json u root direktorijumu servisa
            _storageFilePath = storageFilePath ?? Path.Combine(
                AppDomain.CurrentDomain.BaseDirectory,
                "blockchain_data.json"
            );

            _jsonOptions = new JsonSerializerOptions
            {
                WriteIndented = true, // Čitljiv format za debug
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase
            };
        }

        /// <summary>
        /// Čuva blockchain lanac u JSON fajl.
        /// </summary>
        public void SaveChain(IReadOnlyList<Block> chain)
        {
            try
            {
                var chainData = new
                {
                    Blocks = chain.Select(b => new
                    {
                        b.Index,
                        b.Data,
                        b.Timestamp,
                        b.PreviousHash,
                        b.Difficulty,
                        b.Nonce,
                        b.Hash
                    }).ToList(),
                    SavedAt = DateTimeOffset.UtcNow.ToUnixTimeSeconds()
                };

                string json = JsonSerializer.Serialize(chainData, _jsonOptions);
                File.WriteAllText(_storageFilePath, json);

                Console.WriteLine($"[BlockchainStorage] ✓ Chain saved to {_storageFilePath} ({chain.Count} blocks)");
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"[BlockchainStorage] ✗ Error saving chain: {ex.Message}");
                // Ne baci izuzetak - samo loguj grešku (da ne prekine mining)
            }
        }

        /// <summary>
        /// Učitava blockchain lanac iz JSON fajla.
        /// </summary>
        public List<Block>? LoadChain()
        {
            try
            {
                if (!File.Exists(_storageFilePath))
                {
                    Console.WriteLine($"[BlockchainStorage] No storage file found at {_storageFilePath}, starting with genesis block");
                    return null;
                }

                string json = File.ReadAllText(_storageFilePath);
                var chainData = JsonSerializer.Deserialize<JsonElement>(json);

                if (!chainData.TryGetProperty("blocks", out var blocksElement))
                {
                    Console.Error.WriteLine("[BlockchainStorage] ✗ Invalid storage file format: missing 'blocks' property");
                    return null;
                }

                var blocks = new List<Block>();
                foreach (var blockElement in blocksElement.EnumerateArray())
                {
                    var block = new Block
                    {
                        Index = blockElement.GetProperty("index").GetUInt32(),
                        Data = blockElement.GetProperty("data").GetString() ?? string.Empty,
                        Timestamp = blockElement.GetProperty("timestamp").GetInt64(),
                        PreviousHash = blockElement.GetProperty("previousHash").GetString() ?? "0",
                        Difficulty = blockElement.GetProperty("difficulty").GetUInt32(),
                        Nonce = blockElement.GetProperty("nonce").GetUInt64(),
                        Hash = blockElement.GetProperty("hash").GetString() ?? string.Empty
                    };
                    blocks.Add(block);
                }

                Console.WriteLine($"[BlockchainStorage] ✓ Chain loaded from {_storageFilePath} ({blocks.Count} blocks)");
                return blocks;
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine($"[BlockchainStorage] ✗ Error loading chain: {ex.Message}");
                return null;
            }
        }

        /// <summary>
        /// Proverava da li postoji sačuvani blockchain fajl.
        /// </summary>
        public bool StorageFileExists()
        {
            return File.Exists(_storageFilePath);
        }

        /// <summary>
        /// Vraća putanju do storage fajla.
        /// </summary>
        public string GetStorageFilePath()
        {
            return _storageFilePath;
        }
    }
}

