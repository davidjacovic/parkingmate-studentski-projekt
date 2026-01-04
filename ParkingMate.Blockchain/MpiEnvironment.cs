using System;
using System.Collections.Generic;

namespace ParkingMate.Blockchain
{
    /// <summary>
    /// MPI (Message Passing Interface) okruženje za distribuirano rudarjenje blokova.
    /// Subtask 5.1.1: MPI inicijalizacija - detekcija rank-a i size-a
    /// </summary>
    public class MpiEnvironment
    {
        private static MpiEnvironment? _instance;
        private static readonly object _lock = new object();
        
        private bool _initialized = false;
        private int _rank = 0;
        private int _size = 1;
        private bool _isMaster = true;

        /// <summary>
        /// Privatni konstruktor za singleton pattern
        /// </summary>
        private MpiEnvironment()
        {
        }

        /// <summary>
        /// Singleton instance MPI okruženja
        /// </summary>
        public static MpiEnvironment Instance
        {
            get
            {
                if (_instance == null)
                {
                    lock (_lock)
                    {
                        if (_instance == null)
                        {
                            _instance = new MpiEnvironment();
                        }
                    }
                }
                return _instance;
            }
        }

        /// <summary>
        /// Inicijalizuje MPI okruženje.
        /// Subtask 5.1.1: Inicijalizacija MPI okruženja
        /// </summary>
        /// <param name="size">Ukupan broj procesa (size)</param>
        /// <param name="rank">ID trenutnog procesa (rank)</param>
        /// <returns>True ako je inicijalizacija uspešna</returns>
        public bool Initialize(int size = 1, int rank = 0)
        {
            if (_initialized)
            {
                Console.WriteLine("Upozorenje: MPI okruženje je već inicijalizovano.");
                return false;
            }

            if (size < 1)
            {
                throw new ArgumentException("Size mora biti veći od 0", nameof(size));
            }

            if (rank < 0 || rank >= size)
            {
                throw new ArgumentException($"Rank mora biti između 0 i {size - 1}", nameof(rank));
            }

            _size = size;
            _rank = rank;
            _isMaster = (rank == 0);
            _initialized = true;

            return true;
        }

        /// <summary>
        /// Inicijalizuje MPI okruženje iz environment varijabli ili command-line argumenata.
        /// Simulira inicijalizaciju MPI_COMM_WORLD.
        /// </summary>
        /// <param name="args">Command-line argumenti (mogu sadržati --mpi-size i --mpi-rank)</param>
        /// <returns>True ako je inicijalizacija uspešna</returns>
        public bool InitializeFromArgs(string[]? args = null)
        {
            if (_initialized)
            {
                return false;
            }

            // Pokušaj da učitam iz environment varijabli (kao što bi to bio slučaj sa stvarnim MPI)
            string? mpiSizeEnv = Environment.GetEnvironmentVariable("MPI_SIZE");
            string? mpiRankEnv = Environment.GetEnvironmentVariable("MPI_RANK");

            int size = 1;
            int rank = 0;

            // Parsiraj iz environment varijabli
            if (!string.IsNullOrEmpty(mpiSizeEnv) && int.TryParse(mpiSizeEnv, out int envSize))
            {
                size = envSize;
            }

            if (!string.IsNullOrEmpty(mpiRankEnv) && int.TryParse(mpiRankEnv, out int envRank))
            {
                rank = envRank;
            }

            // Parsiraj iz command-line argumenata ako postoje
            if (args != null)
            {
                for (int i = 0; i < args.Length; i++)
                {
                    string arg = args[i].ToLower();
                    
                    if ((arg == "--mpi-size" || arg == "-ms") && i + 1 < args.Length)
                    {
                        if (int.TryParse(args[i + 1], out int parsedSize) && parsedSize > 0)
                        {
                            size = parsedSize;
                        }
                        i++;
                    }
                    else if ((arg == "--mpi-rank" || arg == "-mr") && i + 1 < args.Length)
                    {
                        if (int.TryParse(args[i + 1], out int parsedRank) && parsedRank >= 0)
                        {
                            rank = parsedRank;
                        }
                        i++;
                    }
                }
            }

            return Initialize(size, rank);
        }

        /// <summary>
        /// Proverava da li je MPI okruženje inicijalizovano
        /// </summary>
        public bool IsInitialized => _initialized;

        /// <summary>
        /// Vraća rank (ID) trenutnog procesa.
        /// Rank 0 je master proces.
        /// Subtask 5.1.2: Detekcija rank-a
        /// </summary>
        public int Rank
        {
            get
            {
                if (!_initialized)
                {
                    throw new InvalidOperationException("MPI okruženje nije inicijalizovano. Pozovi Initialize() prvo.");
                }
                return _rank;
            }
        }

        /// <summary>
        /// Vraća ukupan broj procesa (size).
        /// Subtask 5.1.2: Detekcija size-a
        /// </summary>
        public int Size
        {
            get
            {
                if (!_initialized)
                {
                    throw new InvalidOperationException("MPI okruženje nije inicijalizovano. Pozovi Initialize() prvo.");
                }
                return _size;
            }
        }

        /// <summary>
        /// Proverava da li je trenutni proces master (rank == 0)
        /// </summary>
        public bool IsMaster => _initialized && _isMaster;

        /// <summary>
        /// Proverava da li je trenutni proces worker (rank != 0)
        /// </summary>
        public bool IsWorker => _initialized && !_isMaster;

        /// <summary>
        /// Finalizuje MPI okruženje (cleanup)
        /// </summary>
        public void Finalize()
        {
            if (_initialized)
            {
                _initialized = false;
                _rank = 0;
                _size = 1;
                _isMaster = true;
            }
        }

        /// <summary>
        /// Vraća string reprezentaciju MPI okruženja
        /// </summary>
        public override string ToString()
        {
            if (!_initialized)
            {
                return "MPI Environment: Not initialized";
            }
            return $"MPI Environment: Rank {_rank}/{_size - 1}, Size={_size}, IsMaster={_isMaster}";
        }
    }

    /// <summary>
    /// Helper klasa za proveru da li se program izvršava u MPI modu.
    /// Subtask 5.1.3: MPI flag u CLI interfejsu
    /// </summary>
    public static class MpiHelper
    {
        /// <summary>
        /// Proverava da li je program pokrenut sa MPI flag-om
        /// </summary>
        /// <param name="args">Command-line argumenti</param>
        /// <returns>True ako je --mpi ili -m flag prisutan</returns>
        public static bool IsMpiMode(string[] args)
        {
            if (args == null || args.Length == 0)
            {
                return false;
            }

            foreach (string arg in args)
            {
                string lowerArg = arg.ToLower();
                if (lowerArg == "--mpi" || lowerArg == "-m" || lowerArg == "--use-mpi")
                {
                    return true;
                }
            }

            // Proveri environment varijablu
            string? mpiMode = Environment.GetEnvironmentVariable("MPI_MODE");
            if (!string.IsNullOrEmpty(mpiMode) && (mpiMode.ToLower() == "true" || mpiMode == "1"))
            {
                return true;
            }

            return false;
        }

        /// <summary>
        /// Parsira MPI parametre iz command-line argumenata
        /// </summary>
        /// <param name="args">Command-line argumenti</param>
        /// <returns>Tuple (useMpi, mpiSize, mpiRank)</returns>
        public static (bool useMpi, int? mpiSize, int? mpiRank) ParseMpiArgs(string[] args)
        {
            bool useMpi = IsMpiMode(args);
            int? mpiSize = null;
            int? mpiRank = null;

            if (!useMpi)
            {
                return (false, null, null);
            }

            for (int i = 0; i < args.Length; i++)
            {
                string arg = args[i].ToLower();

                if ((arg == "--mpi-size" || arg == "-ms") && i + 1 < args.Length)
                {
                    if (int.TryParse(args[i + 1], out int size) && size > 0)
                    {
                        mpiSize = size;
                    }
                    i++;
                }
                else if ((arg == "--mpi-rank" || arg == "-mr") && i + 1 < args.Length)
                {
                    if (int.TryParse(args[i + 1], out int rank) && rank >= 0)
                    {
                        mpiRank = rank;
                    }
                    i++;
                }
            }

            return (useMpi, mpiSize, mpiRank);
        }
    }
}

