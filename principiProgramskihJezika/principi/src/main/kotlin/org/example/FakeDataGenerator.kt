package org.example

import java.time.LocalDateTime
import io.github.serpro69.kfaker.Faker
import org.example.LocationCoordinates
import org.example.ParkingLocation
import kotlin.random.Random
import java.time.format.DateTimeFormatter
import org.bson.types.ObjectId
import org.example.User



object FakeDataGenerator {
    private val faker = Faker()
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val now = LocalDateTime.now().format(formatter)

    fun generateFakeParkingLocation(): ParkingLocation {
        val longitude = Random.nextDouble(13.0, 17.0)
        val latitude = Random.nextDouble(45.5, 47.0)

        val totalRegular = (10..100).random()
        val totalInvalid = (0..10).random()
        val totalBus = (0..5).random()

        val availableRegular = (0..totalRegular).random()
        val availableInvalid = (0..totalInvalid).random()
        val availableBus = (0..totalBus).random()

        return ParkingLocation(
            name = faker.company.name(),
            address = faker.address.fullAddress(),
            location = LocationCoordinates(
                coordinates = listOf(longitude, latitude)
            ),
            total_regular_spots = totalRegular,
            total_invalid_spots = totalInvalid,
            total_bus_spots = totalBus,
            available_regular_spots = availableRegular,
            available_invalid_spots = availableInvalid,
            available_bus_spots = availableBus,
            created = LocalDateTime.now(),
            modified = LocalDateTime.now(),
            description = faker.lorem.words(),
            hidden = false
        )
    }


    fun generateFakeUser(): User {
        val firstName = faker.name.firstName()
        val lastName = faker.name.lastName()
        val username = (firstName + lastName).lowercase().take(10) + Random.nextInt(10, 99)
        val email = "${firstName.lowercase()}.${lastName.lowercase()}@example.com"
        val plainPassword = "A${Random.nextInt(1000, 9999)}a@" // Satisfies strong password regex
        val creditCardNumber = List(16) { ('0'..'9').random() }.joinToString("")
        val phoneNumber = (30000000..39999999).random().toString()
        val now = LocalDateTime.now()

        return User(
            name = firstName,
            surname = lastName,
            username = username,
            email = email,
            password_hash = plainPassword,
            phone_number = phoneNumber,
            credit_card_number = creditCardNumber,
            user_type = if ((0..1).random() == 0) "user" else "admin",
            hidden = false,
            created_at = now,
            updated_at = now,
            vehicles = emptyList()
        )
    }


    fun generateFakeVehicleForUser(userId: String, now: LocalDateTime): Vehicle {
        val types = listOf("car", "truck", "motorcycle", "bus")
        val letters = ('A'..'Z').toList()
        val numbers = (1000..9999).random()
        val reg = buildString {
            append(letters.random())
            append(letters.random())
            append(numbers)
            append(letters.random())
            append(letters.random())
        }

        return Vehicle(
            registration_number = reg,
            vehicle_type = types.random(),
            created = now,
            modified = now,
            user = ObjectId(userId),
            hidden = false
        )
    }

    fun generateFakeReview(userId: String, locationId: String, now: LocalDateTime = LocalDateTime.now()): Review {
        val faker = Faker()

        return Review(
            rating = (1..5).random(),
            review_text = faker.lorem.words(),
            review_date = now,
            created = now,
            modified = now,
            hidden = false,
            user = ObjectId(userId),
            parking_location = ObjectId(locationId)
        )
    }
}

