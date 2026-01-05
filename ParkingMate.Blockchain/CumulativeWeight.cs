using System;
using System.Collections.Generic;
using System.Linq;
using System.Numerics;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Klasa za izračunavanje kumulativne težine blockchain lanca (6.2).
    /// Kumulativna težina se izračunava kao suma 2^difficulty za svaki blok u lancu.
    /// </summary>
    public static class CumulativeWeight
    {
        /// <summary>
        /// Izračunava težinu jednog bloka kao 2^difficulty (6.2.1).
        /// 
        /// Primer:
        /// - difficulty = 1 => weight = 2^1 = 2
        /// - difficulty = 2 => weight = 2^2 = 4
        /// - difficulty = 3 => weight = 2^3 = 8
        /// - difficulty = 4 => weight = 2^4 = 16
        /// </summary>
        /// <param name="difficulty">Difficulty vrednost bloka</param>
        /// <returns>Težina bloka kao 2^difficulty (BigInteger za podršku velikim vrednostima)</returns>
        public static BigInteger CalculateBlockWeight(uint difficulty)
        {
            // Izračunaj 2^difficulty
            // Koristimo BigInteger da podržimo velike vrednosti
            return BigInteger.Pow(2, (int)difficulty);
        }

        /// <summary>
        /// Izračunava težinu bloka koristeći Block objekat (6.2.1).
        /// </summary>
        /// <param name="block">Block objekat</param>
        /// <returns>Težina bloka kao 2^difficulty</returns>
        public static BigInteger CalculateBlockWeight(Block block)
        {
            if (block == null)
            {
                throw new ArgumentNullException(nameof(block));
            }

            return CalculateBlockWeight(block.Difficulty);
        }

        /// <summary>
        /// Izračunava kumulativnu težinu lanca sabiranjem težina svih blokova (6.2.2).
        /// Kumulativna težina = suma(2^difficulty) za sve blokove u lancu.
        /// 
        /// Primer:
        /// Lanac sa 3 bloka: difficulty [1, 2, 3]
        /// Težine: [2, 4, 8]
        /// Kumulativna težina = 2 + 4 + 8 = 14
        /// </summary>
        /// <param name="chain">Lista blokova u lancu</param>
        /// <returns>Kumulativna težina lanca (suma 2^difficulty za sve blokove)</returns>
        public static BigInteger CalculateChainWeight(IReadOnlyList<Block> chain)
        {
            if (chain == null)
            {
                throw new ArgumentNullException(nameof(chain));
            }

            if (chain.Count == 0)
            {
                return BigInteger.Zero;
            }

            // Saberi težine svih blokova u lancu
            BigInteger totalWeight = BigInteger.Zero;
            foreach (var block in chain)
            {
                totalWeight += CalculateBlockWeight(block);
            }

            return totalWeight;
        }

        /// <summary>
        /// Izračunava kumulativnu težinu lanca koristeći Blockchain objekat (6.2.2).
        /// </summary>
        /// <param name="blockchain">Blockchain objekat</param>
        /// <returns>Kumulativna težina lanca</returns>
        public static BigInteger CalculateChainWeight(Blockchain blockchain)
        {
            if (blockchain == null)
            {
                throw new ArgumentNullException(nameof(blockchain));
            }

            return CalculateChainWeight(blockchain.Chain);
        }
    }
}

