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
        /// Algoritam prema specifikaciji:
        /// prilagoditvani blok = Veriga blokov[dolžina verige - interval popravka]
        /// pričakovani čas = čas ustvarjanja bloka * interval popravka
        /// dejanski čas = časovna značka zadnjega bloka - časovna značka prilagoditvenega bloka
        /// 
        /// if ( dejanski čas < (pričakovani čas / 2) ) return težavnost prilagoditvenega bloka + 1 
        /// else if ( dejanski čas > (pričakovani čas * 2) ) return težavnost prilagoditvenega bloka - 1
        /// else return težavnost prilagoditvenega bloka
        /// </summary>
        /// <param name="chain">Lista blokova u lancu</param>
        /// <param name="currentDifficulty">Trenutna difficulty vrednost</param>
        /// <param name="blockIntervalSeconds">Ciljano vreme između blokova u sekundama (default: 600 = 10 minuta)</param>
        /// <param name="adjustmentInterval">Broj blokova nakon kojih se prilagođava difficulty (default: 10)</param>
        /// <returns>Nova difficulty vrednost (minimum 1)</returns>
        public static uint CalculateDifficulty(
    IReadOnlyList<Block> chain,
    uint currentDifficulty,
    long blockIntervalSeconds = 600,
    uint adjustmentInterval = 10)
        {
            if (chain == null || chain.Count == 0)
                return currentDifficulty;

            // Ako nema dovoljno blokova da dođemo do chain[length - adjustmentInterval]
            if (chain.Count <= adjustmentInterval)
                return currentDifficulty;

            // Specifikacija: adjustmentBlock = chain[length - adjustmentInterval]
            int adjustmentIndex = chain.Count - (int)adjustmentInterval;
            Block adjustmentBlock = chain[adjustmentIndex];
            Block lastBlock = chain[chain.Count - 1];

            long expected = blockIntervalSeconds * (long)adjustmentInterval;
            long actual = lastBlock.Timestamp - adjustmentBlock.Timestamp;

            // Ako je 0 ili negativno, ne diraj
            if (actual <= 0)
                return currentDifficulty;

            uint baseDifficulty = adjustmentBlock.Difficulty;

            if (actual < expected / 2)
                return baseDifficulty + 1;

            if (actual > expected * 2)
                return baseDifficulty > 1 ? baseDifficulty - 1 : 1;

            return baseDifficulty;
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

