using System;
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
    }
}

