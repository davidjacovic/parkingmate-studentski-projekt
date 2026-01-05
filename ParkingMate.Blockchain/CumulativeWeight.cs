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

        /// <summary>
        /// Rezultat poređenja dva blockchain lanca na osnovu kumulativne težine (6.2.3).
        /// </summary>
        public enum ChainComparisonResult
        {
            FirstHeavier,   // Prvi lanac je teži
            SecondHeavier,  // Drugi lanac je teži
            Equal           // Lanci imaju jednaku težinu
        }

        /// <summary>
        /// Poredi dva blockchain lanca na osnovu njihove kumulativne težine (6.2.3).
        /// </summary>
        /// <param name="chain1">Prvi blockchain lanac</param>
        /// <param name="chain2">Drugi blockchain lanac</param>
        /// <returns>Rezultat poređenja: FirstHeavier, SecondHeavier, ili Equal</returns>
        public static ChainComparisonResult CompareChains(IReadOnlyList<Block> chain1, IReadOnlyList<Block> chain2)
        {
            if (chain1 == null)
            {
                throw new ArgumentNullException(nameof(chain1));
            }
            if (chain2 == null)
            {
                throw new ArgumentNullException(nameof(chain2));
            }

            BigInteger weight1 = CalculateChainWeight(chain1);
            BigInteger weight2 = CalculateChainWeight(chain2);

            if (weight1 > weight2)
            {
                return ChainComparisonResult.FirstHeavier;
            }
            else if (weight2 > weight1)
            {
                return ChainComparisonResult.SecondHeavier;
            }
            else
            {
                return ChainComparisonResult.Equal;
            }
        }

        /// <summary>
        /// Poredi dva blockchain lanca koristeći Blockchain objekte (6.2.3).
        /// </summary>
        /// <param name="blockchain1">Prvi blockchain</param>
        /// <param name="blockchain2">Drugi blockchain</param>
        /// <returns>Rezultat poređenja: FirstHeavier, SecondHeavier, ili Equal</returns>
        public static ChainComparisonResult CompareChains(Blockchain blockchain1, Blockchain blockchain2)
        {
            if (blockchain1 == null)
            {
                throw new ArgumentNullException(nameof(blockchain1));
            }
            if (blockchain2 == null)
            {
                throw new ArgumentNullException(nameof(blockchain2));
            }

            return CompareChains(blockchain1.Chain, blockchain2.Chain);
        }

        /// <summary>
        /// Proverava da li je prvi lanac teži od drugog (6.2.3).
        /// </summary>
        /// <param name="chain1">Prvi blockchain lanac</param>
        /// <param name="chain2">Drugi blockchain lanac</param>
        /// <returns>True ako je prvi lanac teži, inače false</returns>
        public static bool IsFirstChainHeavier(IReadOnlyList<Block> chain1, IReadOnlyList<Block> chain2)
        {
            return CompareChains(chain1, chain2) == ChainComparisonResult.FirstHeavier;
        }

        /// <summary>
        /// Proverava da li je prvi blockchain teži od drugog (6.2.3).
        /// </summary>
        /// <param name="blockchain1">Prvi blockchain</param>
        /// <param name="blockchain2">Drugi blockchain</param>
        /// <returns>True ako je prvi blockchain teži, inače false</returns>
        public static bool IsFirstChainHeavier(Blockchain blockchain1, Blockchain blockchain2)
        {
            return CompareChains(blockchain1, blockchain2) == ChainComparisonResult.FirstHeavier;
        }

        /// <summary>
        /// Biramo najteži lanac iz liste lanaca na osnovu kumulativne težine (6.2.4).
        /// Ako postoji više lanaca sa najvećom težinom, bira se prvi takav lanac.
        /// </summary>
        /// <param name="chains">Lista lanaca za poređenje</param>
        /// <returns>Najteži lanac (ili null ako je lista prazna)</returns>
        public static IReadOnlyList<Block>? SelectHeaviestChain(IReadOnlyList<IReadOnlyList<Block>> chains)
        {
            if (chains == null)
            {
                throw new ArgumentNullException(nameof(chains));
            }

            if (chains.Count == 0)
            {
                return null;
            }

            IReadOnlyList<Block> heaviestChain = chains[0];
            BigInteger maxWeight = CalculateChainWeight(heaviestChain);

            // Pronađi lanac sa najvećom težinom
            for (int i = 1; i < chains.Count; i++)
            {
                if (chains[i] == null)
                {
                    continue; // Preskoči null lanac
                }

                BigInteger currentWeight = CalculateChainWeight(chains[i]);
                if (currentWeight > maxWeight)
                {
                    maxWeight = currentWeight;
                    heaviestChain = chains[i];
                }
            }

            return heaviestChain;
        }

        /// <summary>
        /// Biramo najteži blockchain iz liste blockchain-ova na osnovu kumulativne težine (6.2.4).
        /// Ako postoji više blockchain-ova sa najvećom težinom, bira se prvi takav blockchain.
        /// </summary>
        /// <param name="blockchains">Lista blockchain-ova za poređenje</param>
        /// <returns>Najteži blockchain (ili null ako je lista prazna)</returns>
        public static Blockchain? SelectHeaviestBlockchain(IReadOnlyList<Blockchain> blockchains)
        {
            if (blockchains == null)
            {
                throw new ArgumentNullException(nameof(blockchains));
            }

            if (blockchains.Count == 0)
            {
                return null;
            }

            Blockchain heaviestBlockchain = blockchains[0];
            BigInteger maxWeight = CalculateChainWeight(heaviestBlockchain);

            // Pronađi blockchain sa najvećom težinom
            for (int i = 1; i < blockchains.Count; i++)
            {
                if (blockchains[i] == null)
                {
                    continue; // Preskoči null blockchain
                }

                BigInteger currentWeight = CalculateChainWeight(blockchains[i]);
                if (currentWeight > maxWeight)
                {
                    maxWeight = currentWeight;
                    heaviestBlockchain = blockchains[i];
                }
            }

            return heaviestBlockchain;
        }
    }
}

