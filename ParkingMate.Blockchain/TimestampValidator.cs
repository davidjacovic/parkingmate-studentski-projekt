using System;

namespace ParkingMate.Blockchain
{
    public static class TimestampValidator
    {
        public const long MaxFutureTimeSeconds = 60;
        public const long MaxPastTimeSeconds = 60;

        public static bool IsValidTimestamp(Block currentBlock, Block previousBlock)
        {
            if (currentBlock == null) throw new ArgumentNullException(nameof(currentBlock));
            if (previousBlock == null) throw new ArgumentNullException(nameof(previousBlock));

            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long ts = currentBlock.Timestamp;
            long prevTs = previousBlock.Timestamp;

            // 1) ts <= now + 60
            if (ts > now + MaxFutureTimeSeconds)
                return false;

            // 2) ts >= prevTs - 60  (najviše 1 min manji od prethodnog)
            if (ts < prevTs - MaxPastTimeSeconds)
                return false;

            return true;
        }

        public static bool IsValidTimestamp(
            Block currentBlock,
            Block previousBlock,
            long maxFutureTimeSeconds,
            long maxPastTimeSeconds)
        {
            if (currentBlock == null) throw new ArgumentNullException(nameof(currentBlock));
            if (previousBlock == null) throw new ArgumentNullException(nameof(previousBlock));
            if (maxFutureTimeSeconds < 0) throw new ArgumentException("maxFutureTimeSeconds must be non-negative", nameof(maxFutureTimeSeconds));
            if (maxPastTimeSeconds < 0) throw new ArgumentException("maxPastTimeSeconds must be non-negative", nameof(maxPastTimeSeconds));

            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long ts = currentBlock.Timestamp;
            long prevTs = previousBlock.Timestamp;

            if (ts > now + maxFutureTimeSeconds)
                return false;

            if (ts < prevTs - maxPastTimeSeconds)
                return false;

            return true;
        }

        public static bool IsValidGenesisTimestamp(Block genesisBlock)
        {
            if (genesisBlock == null) throw new ArgumentNullException(nameof(genesisBlock));
            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            return genesisBlock.Timestamp <= now + MaxFutureTimeSeconds;
        }
    }
}
