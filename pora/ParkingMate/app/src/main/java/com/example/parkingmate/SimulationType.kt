package com.example.parkingmate

import java.io.Serializable

enum class SimulationType : Serializable {
    TOTAL_SPACES,
    FREE_SPACES,
    OCCUPIED_SPACES,
    ALL
}