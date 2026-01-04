using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Abstrakcija za MPI komunikaciju.
    /// Omogućava laku zamenu simulacije sa stvarnim MPI pozivima.
    /// Trenutno koristi simulaciju, ali može biti zamenjena stvarnim MPI.NET ili drugom MPI bibliotekom.
    /// </summary>
    public interface IMpiCommunication
    {
        /// <summary>
        /// Šalje poruku procesu sa određenim rank-om.
        /// U stvarnom MPI: MPI_Send
        /// </summary>
        void Send<T>(T data, int destinationRank, int tag = 0) where T : class;

        /// <summary>
        /// Prima poruku od procesa sa određenim rank-om.
        /// U stvarnom MPI: MPI_Recv
        /// </summary>
        T? Receive<T>(int sourceRank, int tag = 0) where T : class;

        /// <summary>
        /// Broadcast poruke sa master procesa na sve procese.
        /// U stvarnom MPI: MPI_Bcast
        /// </summary>
        void Broadcast<T>(ref T data, int rootRank = 0) where T : class;

        /// <summary>
        /// Non-blocking send.
        /// U stvarnom MPI: MPI_Isend
        /// </summary>
        void SendAsync<T>(T data, int destinationRank, int tag = 0) where T : class;

        /// <summary>
        /// Non-blocking receive.
        /// U stvarnom MPI: MPI_Irecv
        /// </summary>
        void ReceiveAsync<T>(int sourceRank, int tag = 0, Action<T>? callback = null) where T : class;
    }

    /// <summary>
    /// Simulacija MPI komunikacije - koristi se za testiranje bez stvarnog MPI okruženja.
    /// Kasnije može biti zamenjena stvarnom MPI implementacijom.
    /// </summary>
    public class SimulatedMpiCommunication : IMpiCommunication
    {
        private readonly MpiEnvironment _mpi;
        private static readonly Dictionary<(int from, int to, int tag), object> _messageQueue = new();
        private static readonly Dictionary<(int rootRank, int tag), object> _broadcastQueue = new();
        private static readonly object _queueLock = new object();

        public SimulatedMpiCommunication(MpiEnvironment mpi)
        {
            _mpi = mpi ?? throw new ArgumentNullException(nameof(mpi));
        }

        public void Send<T>(T data, int destinationRank, int tag = 0) where T : class
        {
            if (data == null) return;

            lock (_queueLock)
            {
                var key = (_mpi.Rank, destinationRank, tag);
                _messageQueue[key] = data;
                Console.WriteLine($"[Rank {_mpi.Rank}] Simulacija MPI_Send: Šaljem poruku rank-u {destinationRank}, tag={tag}");
            }
        }

        public T? Receive<T>(int sourceRank, int tag = 0) where T : class
        {
            lock (_queueLock)
            {
                var key = (sourceRank, _mpi.Rank, tag);
                if (_messageQueue.TryGetValue(key, out var message) && message is T typedMessage)
                {
                    _messageQueue.Remove(key);
                    Console.WriteLine($"[Rank {_mpi.Rank}] Simulacija MPI_Recv: Primio poruku od rank-a {sourceRank}, tag={tag}");
                    return typedMessage;
                }
                
                Console.WriteLine($"[Rank {_mpi.Rank}] Simulacija MPI_Recv: Nema poruke od rank-a {sourceRank}, tag={tag}");
                return null;
            }
        }

        public void Broadcast<T>(ref T data, int rootRank = 0) where T : class
        {
            lock (_queueLock)
            {
                var key = (rootRank, tag: 0); // Broadcast koristi tag 0 ili poseban tag
                
                if (_mpi.Rank == rootRank)
                {
                    // Root rank šalje broadcast
                    _broadcastQueue[key] = data;
                    Console.WriteLine($"[Rank {_mpi.Rank}] Simulacija MPI_Bcast: Šaljem broadcast sa root rank-a {rootRank}");
                }
                else
                {
                    // Ostali ranks primaju broadcast
                    if (_broadcastQueue.TryGetValue(key, out var broadcastData) && broadcastData is T typedData)
                    {
                        data = typedData;
                        Console.WriteLine($"[Rank {_mpi.Rank}] Simulacija MPI_Bcast: Primio broadcast od root rank-a {rootRank}");
                    }
                    else
                    {
                        Console.WriteLine($"[Rank {_mpi.Rank}] Simulacija MPI_Bcast: Nema broadcast poruke od root rank-a {rootRank}");
                    }
                }
            }
        }

        public void SendAsync<T>(T data, int destinationRank, int tag = 0) where T : class
        {
            // Za simulaciju, async je isto kao sync
            Send(data, destinationRank, tag);
        }

        public void ReceiveAsync<T>(int sourceRank, int tag = 0, Action<T>? callback = null) where T : class
        {
            // Za simulaciju, async je isto kao sync
            var message = Receive<T>(sourceRank, tag);
            callback?.Invoke(message!);
        }

        /// <summary>
        /// Očisti message queue i broadcast queue (za testiranje)
        /// </summary>
        public static void ClearQueue()
        {
            lock (_queueLock)
            {
                _messageQueue.Clear();
                _broadcastQueue.Clear();
            }
        }
    }

    /// <summary>
    /// Stvarna MPI komunikacija - za buduću integraciju sa MPI.NET ili drugom MPI bibliotekom.
    /// 
    /// Primer implementacije sa MPI.NET:
    /// 
    /// public class RealMpiCommunication : IMpiCommunication
    /// {
    ///     private readonly Intracommunicator comm;
    ///     
    ///     public RealMpiCommunication(Intracommunicator communicator)
    ///     {
    ///         comm = communicator;
    ///     }
    ///     
    ///     public void Send<T>(T data, int destinationRank, int tag = 0)
    ///     {
    ///         comm.Send(data, destinationRank, tag);
    ///     }
    ///     
    ///     public T? Receive<T>(int sourceRank, int tag = 0)
    ///     {
    ///         return comm.Receive<T>(sourceRank, tag);
    ///     }
    ///     
    ///     public void Broadcast<T>(ref T data, int rootRank = 0)
    ///     {
    ///         comm.Broadcast(ref data, rootRank);
    ///     }
    ///     
    ///     // ... implementacija ostalih metoda
    /// }
    /// 
    /// Primer korišćenja:
    /// 
    /// // Inicijalizacija MPI.NET
    /// using (new MPI.Environment(ref args))
    /// {
    ///     var comm = Communicator.world;
    ///     var mpiComm = new RealMpiCommunication(comm);
    ///     // ... koristi mpiComm
    /// }
    /// </summary>
    public class RealMpiCommunication : IMpiCommunication
    {
        // TODO: Implementacija sa stvarnom MPI bibliotekom (MPI.NET, SharpMPI, itd.)
        // Ova klasa će biti implementirana kada se doda stvarni MPI support

        public RealMpiCommunication()
        {
            throw new NotImplementedException("RealMpiCommunication zahteva instalaciju MPI biblioteke (MPI.NET ili slično)");
        }

        public void Send<T>(T data, int destinationRank, int tag = 0) where T : class
        {
            throw new NotImplementedException("Implementiraj sa stvarnim MPI_Send");
            // Primer sa MPI.NET:
            // Intracommunicator comm = ...;
            // comm.Send(data, destinationRank, tag);
        }

        public T? Receive<T>(int sourceRank, int tag = 0) where T : class
        {
            throw new NotImplementedException("Implementiraj sa stvarnim MPI_Recv");
            // Primer sa MPI.NET:
            // Intracommunicator comm = ...;
            // return comm.Receive<T>(sourceRank, tag);
        }

        public void Broadcast<T>(ref T data, int rootRank = 0) where T : class
        {
            throw new NotImplementedException("Implementiraj sa stvarnim MPI_Bcast");
            // Primer sa MPI.NET:
            // Intracommunicator comm = ...;
            // comm.Broadcast(ref data, rootRank);
        }

        public void SendAsync<T>(T data, int destinationRank, int tag = 0) where T : class
        {
            throw new NotImplementedException("Implementiraj sa stvarnim MPI_Isend");
        }

        public void ReceiveAsync<T>(int sourceRank, int tag = 0, Action<T>? callback = null) where T : class
        {
            throw new NotImplementedException("Implementiraj sa stvarnim MPI_Irecv");
        }
    }
}

