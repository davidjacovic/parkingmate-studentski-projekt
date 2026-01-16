using System.Threading;

namespace ParkingMate.Blockchain.Infrastructure
{
    /// <summary>
    /// Gate za sprečavanje paralelnog mining-a (kasnije Task D -> 409).
    /// </summary>
    public class MiningGate
    {
        private readonly SemaphoreSlim _sem = new SemaphoreSlim(1, 1);

        public Task<bool> TryEnterAsync(TimeSpan timeout) => _sem.WaitAsync(timeout);

        public void Exit() => _sem.Release();
    }
}
