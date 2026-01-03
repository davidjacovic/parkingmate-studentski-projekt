package com.example.parkingmate

import java.io.Serializable


enum class EventType : Serializable {
    PARKING_FULL,
    PARKING_AVAILABLE,
    LOW_AVAILABILITY
}
