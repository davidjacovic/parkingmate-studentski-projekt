using System;
using System.Threading;
using System.Threading.Tasks;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Dizajn za paralelno rudarjenje blokova korišćenjem više niti.
    /// Subtask 4.1.1: Dizajn podele nonce prostora po nitima
    /// </summary>
    public class ThreadedMiner
    {
        /// <summary>
        /// Dizajn podele nonce prostora:
        /// - Nonce prostor (ulong: 0 do 18,446,744,073,709,551,615) se deli na N opsega
        /// - Svaka nit dobija jedinstven opseg: [startNonce, endNonce)
        /// - Opseg se računa kao: threadId * (MAX_NONCE / numThreads) do (threadId + 1) * (MAX_NONCE / numThreads)
        /// </summary>
        /// <param name="numThreads">Broj niti</param>
        /// <param name="threadId">ID trenutne niti (0-based)</param>
        /// <returns>Par (startNonce, endNonce) za datu nit</returns>
        public static (ulong startNonce, ulong endNonce) CalculateNonceRange(int numThreads, int threadId)
        {
            // Dizajn: Podela nonce prostora na jednak broj opsega
            // Thread 0: [0, MAX/N)
            // Thread 1: [MAX/N, 2*MAX/N)
            // Thread 2: [2*MAX/N, 3*MAX/N)
            // ...
            // Thread N-1: [(N-1)*MAX/N, MAX]
            
            if (numThreads <= 0)
                throw new ArgumentException("Broj niti mora biti veći od 0", nameof(numThreads));
            
            if (threadId < 0 || threadId >= numThreads)
                throw new ArgumentException($"Thread ID mora biti između 0 i {numThreads - 1}", nameof(threadId));

            ulong maxNonce = ulong.MaxValue;
            ulong rangeSize = maxNonce / (ulong)numThreads;
            
            ulong startNonce = (ulong)threadId * rangeSize;
            
            // Poslednja nit dobija sve preostale vrednosti do ulong.MaxValue
            ulong endNonce = (threadId == numThreads - 1) 
                ? ulong.MaxValue 
                : ((ulong)(threadId + 1) * rangeSize);

            return (startNonce, endNonce);
        }

        // TODO: Implementacija će biti dodata u narednim subtaskovima (4.1.2, 4.1.3, itd.)
    }
}


