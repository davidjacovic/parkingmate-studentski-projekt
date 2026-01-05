using System;
using System.Collections.Generic;
using System.Linq;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Implementacija time-based algoritma za dinamičko prilagođavanje difficulty-ja (6.1.1).
    /// Algoritam prilagođava difficulty na osnovu vremena potrebnog za mining blokova.
    /// </summary>
    public static class DynamicDifficulty
    {
        /// <summary>
        /// Izračunava novu difficulty vrednost na osnovu vremena potrebnog za mining poslednjih N blokova.
        /// 
        /// Algoritam:
        /// 1. Uzima poslednjih 'adjustmentInterval' blokova (ili sve dostupne blokove ako je lanac kraći)
        /// 2. Računa ukupno vreme između prvog i poslednjeg bloka u intervalu
        /// 3. Upoređuje sa ciljanim vremenom (adjustmentInterval * blockIntervalSeconds)
        /// 4. Prilagođava difficulty na osnovu omjera stvarnog i ciljanog vremena
        /// 
        /// Formula: newDifficulty = currentDifficulty * (actualTime / targetTime)
        /// 
        /// Ako je mining bio brži od ciljanog vremena -> povećava se difficulty
        /// Ako je mining bio sporiji od ciljanog vremena -> smanjuje se difficulty
        /// </summary>
        /// <param name="chain">Lista blokova u lancu</param>
        /// <param name="currentDifficulty">Trenutna difficulty vrednost</param>
        /// <param name="blockIntervalSeconds">Ciljano vreme između blokova u sekundama (default: 600 = 10 minuta)</param>
        /// <param name="adjustmentInterval">Broj blokova nakon kojih se prilagođava difficulty (default: 10)</param>
        /// <returns>Nova difficulty vrednost (minimum 1)</returns>
        public static uint CalculateDifficulty(
            IReadOnlyList<Block> chain,
            uint currentDifficulty,
            long blockIntervalSeconds = 600, // Default: 10 minuta
            uint adjustmentInterval = 10)    // Default: 10 blokova
        {
            if (chain == null || chain.Count == 0)
            {
                return currentDifficulty;
            }

            // Ako nema dovoljno blokova za prilagođavanje, vraća trenutnu difficulty
            if (chain.Count <= 1)
            {
                return currentDifficulty;
            }

            // Uzmi poslednjih 'adjustmentInterval' blokova (ili sve ako je lanac kraći)
            int blocksToConsider = Math.Min((int)adjustmentInterval, chain.Count - 1);
            
            // Prvi blok u intervalu (najstariji)
            Block firstBlock = chain[chain.Count - blocksToConsider - 1];
            // Poslednji blok u intervalu (najnoviji)
            Block lastBlock = chain[chain.Count - 1];

            // Izračunaj ukupno vreme između prvog i poslednjeg bloka (u sekundama)
            long actualTimeSeconds = lastBlock.Timestamp - firstBlock.Timestamp;

            // Ako je vreme 0 ili negativno, ne prilagođavaj difficulty
            if (actualTimeSeconds <= 0)
            {
                return currentDifficulty;
            }

            // Ciljano vreme za mining 'blocksToConsider' blokova
            long targetTimeSeconds = blocksToConsider * blockIntervalSeconds;

            // Izračunaj omjer ciljanog i stvarnog vremena
            // Formula: newDifficulty = currentDifficulty * (targetTime / actualTime)
            // Ako je mining bio brži (actualTime < targetTime), timeRatio > 1.0, difficulty se povećava
            // Ako je mining bio sporiji (actualTime > targetTime), timeRatio < 1.0, difficulty se smanjuje
            double timeRatio = (double)targetTimeSeconds / actualTimeSeconds;

            // Prilagođi difficulty na osnovu omjera
            double newDifficultyDouble = currentDifficulty * timeRatio;

            // Zaokruži na najbliži ceo broj
            uint newDifficulty = (uint)Math.Round(newDifficultyDouble);

            // Osiguraj minimum difficulty od 1
            if (newDifficulty < 1)
            {
                newDifficulty = 1;
            }

            return newDifficulty;
        }

        /// <summary>
        /// Proverava da li je vreme za prilagođavanje difficulty-ja.
        /// Vraća true ako je broj blokova u lancu deljiv sa adjustmentInterval (posle genesis bloka).
        /// </summary>
        /// <param name="chainLength">Dužina lanca (uključujući genesis blok)</param>
        /// <param name="adjustmentInterval">Interval za prilagođavanje</param>
        /// <returns>True ako je vreme za prilagođavanje</returns>
        public static bool ShouldAdjustDifficulty(int chainLength, uint adjustmentInterval)
        {
            // Prilagođavaj difficulty svakih 'adjustmentInterval' blokova (posle genesis bloka)
            // Genesis blok (index 0) se ne računa
            int blocksSinceGenesis = chainLength - 1;
            
            if (blocksSinceGenesis <= 0)
            {
                return false;
            }

            // Prilagođavaj na početku svakog adjustment intervala
            return blocksSinceGenesis % adjustmentInterval == 0;
        }

        /// <summary>
        /// Izračunava prosečno vreme između blokova u sekundama.
        /// </summary>
        /// <param name="chain">Lista blokova</param>
        /// <param name="numBlocks">Broj poslednjih blokova za analizu (0 = svi blokovi)</param>
        /// <returns>Prosečno vreme između blokova u sekundama, ili 0 ako nema dovoljno blokova</returns>
        public static double GetAverageBlockTime(IReadOnlyList<Block> chain, int numBlocks = 0)
        {
            if (chain == null || chain.Count <= 1)
            {
                return 0.0;
            }

            int blocksToConsider = numBlocks > 0 ? Math.Min(numBlocks, chain.Count - 1) : chain.Count - 1;
            
            if (blocksToConsider <= 0)
            {
                return 0.0;
            }

            Block firstBlock = chain[chain.Count - blocksToConsider - 1];
            Block lastBlock = chain[chain.Count - 1];

            long totalTimeSeconds = lastBlock.Timestamp - firstBlock.Timestamp;
            
            if (totalTimeSeconds <= 0 || blocksToConsider == 0)
            {
                return 0.0;
            }

            return (double)totalTimeSeconds / blocksToConsider;
        }
    }
}

