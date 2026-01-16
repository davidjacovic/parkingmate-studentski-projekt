using System;
using MPI;

namespace ParkingMate.Blockchain
{
    public interface IMpiCommunication
    {
        void Send<T>(T data, int destinationRank, int tag = 0);
        T Receive<T>(int sourceRank, int tag = 0);

        // ✅ ANY_SOURCE podrška (real MPI)
        T ReceiveAnySource<T>(int tag, out int actualSourceRank);

        void Barrier();
    }

    public class RealMpiCommunication : IMpiCommunication
    {
        private readonly Intracommunicator _comm;

        public RealMpiCommunication(Intracommunicator comm)
        {
            _comm = comm ?? throw new ArgumentNullException(nameof(comm));
        }

        public void Send<T>(T data, int destinationRank, int tag = 0)
            => _comm.Send(data, destinationRank, tag);

        public T Receive<T>(int sourceRank, int tag = 0)
            => _comm.Receive<T>(sourceRank, tag);

        public T ReceiveAnySource<T>(int tag, out int actualSourceRank)
        {
            // 1️⃣ Čekaj poruku od bilo kog source-a
            Status status = _comm.Probe(Communicator.anySource, tag);

            // 2️⃣ Zapamti ko je poslao
            actualSourceRank = status.Source;

            // 3️⃣ Primi poruku od tog source-a
            return _comm.Receive<T>(actualSourceRank, tag);
        }

        public void Barrier()
            => _comm.Barrier();
    }
}
