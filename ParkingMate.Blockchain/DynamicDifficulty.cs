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

            // Prema specifikaciji:
            // prilagoditvani blok = Veriga blokov[dolžina verige - interval popravka]
            // pričakovani čas = čas ustvarjanja bloka * interval popravka
            // dejanski čas = časovna značka zadnjega bloka - časovna značka prilagoditvenega bloka
            
            // Pronađi prilagoditveni blok (blok na poziciji chain.Count - adjustmentInterval)
            // Napomena: Genesis blok (index 0) se ne računa u interval, tako da uzimamo blokove posle genesis bloka
            int adjustmentBlockIndex = chain.Count - (int)adjustmentInterval - 1;
            
            // Proveri da li postoji dovoljno blokova (mora biti bar 1 blok posle genesis bloka)
            if (adjustmentBlockIndex < 1)
            {
                // Nema dovoljno blokova za prilagođavanje (genesis blok se ne računa)
                return currentDifficulty;
            }
            
            Block adjustmentBlock = chain[adjustmentBlockIndex];
            Block lastBlock = chain[chain.Count - 1];
            
            // Pričakovani čas = čas ustvarjanja bloka * interval popravka
            long expectedTimeSeconds = blockIntervalSeconds * adjustmentInterval;
            
            // Dejanski čas = časovna značka zadnjega bloka - časovna značka prilagoditvenega bloka
            long actualTimeSeconds = lastBlock.Timestamp - adjustmentBlock.Timestamp;
            
            // Ako je vreme 0 ili negativno, ne prilagođavaj difficulty
            if (actualTimeSeconds <= 0)
            {
                Console.WriteLine($"[Difficulty] Ne prilagođava se: actualTimeSeconds={actualTimeSeconds} (mora biti > 0)");
                return currentDifficulty;
            }
            
            // Koristimo difficulty poslednjeg bloka u intervalu (ne prilagoditvenog bloka)
            // jer je to difficulty koja je korišćena za mining blokova u intervalu
            uint baseDifficulty = lastBlock.Difficulty;
            
            // Debug: Prikaži informacije o prilagođavanju
            Console.WriteLine($"[Difficulty Debug] Analiza po specifikaciji:");
            Console.WriteLine($"  Prilagoditveni blok: Index {adjustmentBlock.Index}, Difficulty {adjustmentBlock.Difficulty}");
            Console.WriteLine($"  Poslednji blok: Index {lastBlock.Index}, Difficulty {lastBlock.Difficulty}");
            Console.WriteLine($"  Pričakovani čas: {expectedTimeSeconds}s ({adjustmentInterval} blokova × {blockIntervalSeconds}s po bloku)");
            Console.WriteLine($"  Dejanski čas: {actualTimeSeconds}s (od bloka {adjustmentBlock.Index} do {lastBlock.Index})");
            Console.WriteLine($"  Bazna difficulty za prilagođavanje: {baseDifficulty} (difficulty poslednjeg bloka)");
            Console.WriteLine($"  Poređenje:");
            Console.WriteLine($"    - 2x brži ili brži: {actualTimeSeconds}s <= {expectedTimeSeconds / 2}s? {(actualTimeSeconds <= (expectedTimeSeconds / 2) ? "DA" : "NE")}");
            Console.WriteLine($"    - 2x sporiji ili sporiji: {actualTimeSeconds}s >= {expectedTimeSeconds * 2}s? {(actualTimeSeconds >= (expectedTimeSeconds * 2) ? "DA" : "NE")}");
            
            // Prema specifikaciji:
            // if ( dejanski čas < (pričakovani čas / 2) ) return težavnost prilagoditvenega bloka + 1 
            // else if ( dejanski čas > (pričakovani čas * 2) ) return težavnost prilagoditvenega bloka - 1
            // else return težavnost prilagoditvenega bloka
            // 
            // Napomena: Koristimo difficulty poslednjeg bloka kao baznu, jer je to difficulty koja je korišćena za mining
            
            uint newDifficulty;
            
            // Provera: da li je mining bio 2x brži ili brži (<= polovina pričakovanog vremena)
            if (actualTimeSeconds <= (expectedTimeSeconds / 2))
            {
                // Mining je bio 2x brži ili brži - povećaj difficulty za 1
                newDifficulty = baseDifficulty + 1;
                Console.WriteLine($"  Mining je bio 2x brži ili brži ({actualTimeSeconds}s <= {expectedTimeSeconds / 2}s)");
                Console.WriteLine($"  Difficulty povećana: {baseDifficulty} → {newDifficulty} (+1)");
            }
            // Provera: da li je mining bio 2x sporiji ili sporiji (>= duplo pričakovano vreme)
            else if (actualTimeSeconds >= (expectedTimeSeconds * 2))
            {
                // Mining je bio 2x sporiji ili sporiji - smanji difficulty za 1
                newDifficulty = baseDifficulty > 1 ? baseDifficulty - 1 : 1;
                Console.WriteLine($"  Mining je bio 2x sporiji ili sporiji ({actualTimeSeconds}s >= {expectedTimeSeconds * 2}s)");
                Console.WriteLine($"  Difficulty smanjena: {baseDifficulty} → {newDifficulty} (-1)");
            }
            else
            {
                // Mining je bio u normalnom opsegu - zadrži difficulty
                newDifficulty = baseDifficulty;
                Console.WriteLine($"  Mining je bio u normalnom opsegu");
                Console.WriteLine($"  Difficulty ostaje ista: {newDifficulty}");
            }
            
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

