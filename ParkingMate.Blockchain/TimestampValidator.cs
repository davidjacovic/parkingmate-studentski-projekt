using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Klasa za validaciju timestamp-a blokova (TASK 2.3).
    /// </summary>
    public static class TimestampValidator
    {
        /// <summary>
        /// Maksimalno dozvoljeno vreme u budućnosti (u sekundama).
        /// Prema specifikaciji: "blok je ustrezen, če je njegova časovna značka največ 1 minuto večja od našega trenutnega časa"
        /// Default: 1 minuta (60 sekundi).
        /// </summary>
        public const long MaxFutureTimeSeconds = 60;

        /// <summary>
        /// Maksimalno dozvoljeno vreme u prošlosti u odnosu na prethodni blok (u sekundama).
        /// Prema specifikaciji: "blok v verigi je ustrezen če je njegova časovna značka največ 1 minuto manjša od časovne značke prejšnjega bloka"
        /// Default: 1 minuta (60 sekundi).
        /// </summary>
        public const long MaxPastTimeSeconds = 60;

        /// <summary>
        /// Validira timestamp novog bloka u odnosu na trenutno vreme i prethodni blok (TASK 2.3).
        /// </summary>
        /// <param name="currentBlock">Trenutni blok koji se validira</param>
        /// <param name="previousBlock">Prethodni blok u lancu</param>
        /// <returns>True ako je timestamp validan, false inače</returns>
        public static bool IsValidTimestamp(Block currentBlock, Block previousBlock)
        {
            if (currentBlock == null)
                throw new ArgumentNullException(nameof(currentBlock));
            if (previousBlock == null)
                throw new ArgumentNullException(nameof(previousBlock));

            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long blockTimestamp = currentBlock.Timestamp;

            // 1. Provera: Timestamp ne sme biti previše u budućnosti
            if (blockTimestamp > currentTime + MaxFutureTimeSeconds)
            {
                return false;
            }

            // 2. Provera: Timestamp mora biti veći od prethodnog bloka
            // (blokovi moraju biti u hronološkom redosledu)
            if (blockTimestamp <= previousBlock.Timestamp)
            {
                return false;
            }

            // 3. Provera: Timestamp ne sme biti previše u prošlosti u odnosu na prethodni blok
            // Prema specifikaciji: "blok v verigi je ustrezen če je njegova časovna značka največ 1 minuto manjša od časovne značke prejšnjega bloka"
            // To znači da razlika između prethodnog i trenutnog bloka ne sme biti veća od 1 minute
            // Ali pošto je blockTimestamp > previousBlock.Timestamp (provera 2), ovo proverava da li je razlika prevelika
            long timeDifference = blockTimestamp - previousBlock.Timestamp;
            if (timeDifference > MaxPastTimeSeconds)
            {
                return false;
            }

            return true;
        }

        /// <summary>
        /// Validira timestamp novog bloka sa custom parametrima (TASK 2.3).
        /// </summary>
        /// <param name="currentBlock">Trenutni blok koji se validira</param>
        /// <param name="previousBlock">Prethodni blok u lancu</param>
        /// <param name="maxFutureTimeSeconds">Maksimalno dozvoljeno vreme u budućnosti</param>
        /// <param name="maxPastTimeSeconds">Maksimalno dozvoljeno vreme u prošlosti</param>
        /// <returns>True ako je timestamp validan, false inače</returns>
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

            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long blockTimestamp = currentBlock.Timestamp;

            // 1. Provera: Timestamp ne sme biti previše u budućnosti
            if (blockTimestamp > currentTime + maxFutureTimeSeconds)
            {
                return false;
            }

            // 2. Provera: Timestamp mora biti veći od prethodnog bloka
            if (blockTimestamp <= previousBlock.Timestamp)
            {
                return false;
            }

            // 3. Provera: Timestamp ne sme biti previše u prošlosti u odnosu na prethodni blok
            long timeDifference = previousBlock.Timestamp - blockTimestamp;
            if (timeDifference > maxPastTimeSeconds)
            {
                return false;
            }

            return true;
        }

        /// <summary>
        /// Validira timestamp za genesis blok (genesis blok može imati bilo koji timestamp).
        /// </summary>
        /// <param name="genesisBlock">Genesis blok</param>
        /// <returns>True ako je timestamp validan, false inače</returns>
        public static bool IsValidGenesisTimestamp(Block genesisBlock)
        {
            if (genesisBlock == null)
                throw new ArgumentNullException(nameof(genesisBlock));

            // Genesis blok može imati bilo koji timestamp (čak i u prošlosti)
            // Jedina provera je da ne sme biti previše u budućnosti
            long currentTime = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long blockTimestamp = genesisBlock.Timestamp;

            return blockTimestamp <= currentTime + MaxFutureTimeSeconds;
        }
    }
}

