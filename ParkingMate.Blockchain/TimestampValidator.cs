using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Klasa za validaciju timestamp-a blokova (TASK 2.3).
    /// </summary>
    public static class TimestampValidator
    {
        public const long MaxFutureTimeSeconds = 60;
        public const long MaxPastTimeSeconds = 60;

        public static bool IsValidTimestamp(Block currentBlock, Block previousBlock)
        {
            if (currentBlock == null)
                throw new ArgumentNullException(nameof(currentBlock));
            if (previousBlock == null)
                throw new ArgumentNullException(nameof(previousBlock));

            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long ts = currentBlock.Timestamp;
            long prevTs = previousBlock.Timestamp;

            // 1) Timestamp ne sme biti previše u budućnosti u odnosu na "naše trenutno vreme"
            if (ts > now + MaxFutureTimeSeconds)
                return false;

            // !!! PROMENA !!! (TASK 2.3)
            // Prema specifikaciji: blok je OK ako je njegova časovna značka najviše 1 minut MANJA od prethodnog bloka.
            // Dakle dozvoljavamo: ts == prevTs i ts može biti malo manji (do 60s).
            // Staro: if (ts <= prevTs) return false;  (prestrogo - ruši mining kad timestamp padne u istu sekundu)
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
            if (currentBlock == null)
                throw new ArgumentNullException(nameof(currentBlock));
            if (previousBlock == null)
                throw new ArgumentNullException(nameof(previousBlock));
            if (maxFutureTimeSeconds < 0)
                throw new ArgumentException("maxFutureTimeSeconds must be non-negative", nameof(maxFutureTimeSeconds));
            if (maxPastTimeSeconds < 0)
                throw new ArgumentException("maxPastTimeSeconds must be non-negative", nameof(maxPastTimeSeconds));

            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long ts = currentBlock.Timestamp;
            long prevTs = previousBlock.Timestamp;

            // 1) Timestamp ne sme biti previše u budućnosti
            if (ts > now + maxFutureTimeSeconds)
                return false;

            // !!! PROMENA !!! (TASK 2.3 - custom parametri)
            // Dozvoljavamo ts == prevTs i ts >= prevTs - maxPastTimeSeconds
            // Staro je imalo 2 greške:
            //  - tražilo ts > prevTs (prestrogo)
            //  - računalo timeDifference = prevTs - ts (pogrešan smer u tvojoj staroj default verziji)
            if (ts < prevTs - maxPastTimeSeconds)
                return false;

            return true;
        }

        public static bool IsValidGenesisTimestamp(Block genesisBlock)
        {
            if (genesisBlock == null)
                throw new ArgumentNullException(nameof(genesisBlock));

            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long ts = genesisBlock.Timestamp;

            return ts <= now + MaxFutureTimeSeconds;
        }
    }
}
