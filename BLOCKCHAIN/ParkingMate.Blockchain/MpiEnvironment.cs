using System;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// Legacy/simulacioni MPI "environment" (nije MPI.NET).
    /// Zadržan samo da testovi koji ga koriste mogu da se kompajliraju.
    ///
    /// U REAL MPI modu koristi se MPI.Environment + Communicator.world (vidi Program.cs).
    /// </summary>
    public class MpiEnvironment
    {
        private static MpiEnvironment? _instance;
        private static readonly object _lock = new object();

        private bool _initialized = false;
        private int _rank = 0;
        private int _size = 1;

        private MpiEnvironment() { }

        public static MpiEnvironment Instance
        {
            get
            {
                if (_instance == null)
                {
                    lock (_lock)
                    {
                        _instance ??= new MpiEnvironment();
                    }
                }
                return _instance;
            }
        }

        public bool Initialize(int size = 1, int rank = 0)
        {
            if (_initialized)
            {
                Console.WriteLine("Upozorenje: MPI okruženje je već inicijalizovano (legacy).");
                return false;
            }

            if (size < 1)
                throw new ArgumentException("Size mora biti > 0", nameof(size));

            if (rank < 0 || rank >= size)
                throw new ArgumentException($"Rank mora biti između 0 i {size - 1}", nameof(rank));

            _size = size;
            _rank = rank;
            _initialized = true;

            return true;
        }

        public bool InitializeFromArgs(string[]? args = null)
        {
            if (_initialized) return false;

            // Legacy: čita (opciono) iz env var ili CLI arg (simulacija)
            string? mpiSizeEnv = Environment.GetEnvironmentVariable("MPI_SIZE");
            string? mpiRankEnv = Environment.GetEnvironmentVariable("MPI_RANK");

            int size = 1;
            int rank = 0;

            if (!string.IsNullOrEmpty(mpiSizeEnv) && int.TryParse(mpiSizeEnv, out int envSize) && envSize > 0)
                size = envSize;

            if (!string.IsNullOrEmpty(mpiRankEnv) && int.TryParse(mpiRankEnv, out int envRank) && envRank >= 0)
                rank = envRank;

            if (args != null)
            {
                for (int i = 0; i < args.Length; i++)
                {
                    string arg = args[i].ToLowerInvariant();

                    if ((arg == "--mpi-size" || arg == "-ms") && i + 1 < args.Length)
                    {
                        if (int.TryParse(args[i + 1], out int parsedSize) && parsedSize > 0)
                            size = parsedSize;
                        i++;
                    }
                    else if ((arg == "--mpi-rank" || arg == "-mr") && i + 1 < args.Length)
                    {
                        if (int.TryParse(args[i + 1], out int parsedRank) && parsedRank >= 0)
                            rank = parsedRank;
                        i++;
                    }
                }
            }

            return Initialize(size, rank);
        }

        public bool IsInitialized => _initialized;

        public int Rank
        {
            get
            {
                if (!_initialized) throw new InvalidOperationException("MPI okruženje nije inicijalizovano (legacy).");
                return _rank;
            }
        }

        public int Size
        {
            get
            {
                if (!_initialized) throw new InvalidOperationException("MPI okruženje nije inicijalizovano (legacy).");
                return _size;
            }
        }

        public bool IsMaster => _initialized && _rank == 0;
        public bool IsWorker => _initialized && _rank != 0;

        /// <summary>
        /// Cleanup za legacy/simulacioni env. Ne dira real MPI (MPI.Environment).
        /// </summary>
        public void Shutdown()
        {
            if (!_initialized) return;

            int oldRank = _rank;

            _initialized = false;
            _rank = 0;
            _size = 1;

            Console.WriteLine($"[Legacy MPI Rank {oldRank}] ✓ Legacy MPI env resetovan.");
        }

        public override string ToString()
        {
            if (!_initialized) return "MPI Environment (legacy): Not initialized";
            return $"MPI Environment (legacy): Rank {_rank}/{_size - 1}, Size={_size}, IsMaster={IsMaster}";
        }
    }

    /// <summary>
    /// Helper klasa za CLI detekciju MPI moda.
    /// (Ovo možeš da koristiš, ali realno u MPI.NET modu world.Size već govori sve.)
    /// </summary>
    public static class MpiHelper
    {
        public static bool IsMpiMode(string[] args)
        {
            if (args == null || args.Length == 0)
                return false;

            foreach (string arg in args)
            {
                string lowerArg = arg.ToLowerInvariant();
                if (lowerArg == "--mpi" || lowerArg == "-m" || lowerArg == "--use-mpi")
                    return true;
            }

            string? mpiMode = Environment.GetEnvironmentVariable("MPI_MODE");
            if (!string.IsNullOrEmpty(mpiMode) && (mpiMode.Equals("true", StringComparison.OrdinalIgnoreCase) || mpiMode == "1"))
                return true;

            return false;
        }

        public static (bool useMpi, int? mpiSize, int? mpiRank) ParseMpiArgs(string[] args)
        {
            bool useMpi = IsMpiMode(args);
            int? mpiSize = null;
            int? mpiRank = null;

            if (!useMpi)
                return (false, null, null);

            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i].ToLowerInvariant();

                if ((arg == "--mpi-size" || arg == "-ms") && i + 1 < args.Length)
                {
                    if (int.TryParse(args[i + 1], out int size) && size > 0)
                        mpiSize = size;
                    i++;
                }
                else if ((arg == "--mpi-rank" || arg == "-mr") && i + 1 < args.Length)
                {
                    if (int.TryParse(args[i + 1], out int rank) && rank >= 0)
                        mpiRank = rank;
                    i++;
                }
            }

            return (useMpi, mpiSize, mpiRank);
        }
    }
}
