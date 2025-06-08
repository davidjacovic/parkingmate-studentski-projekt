package org.example

import java.time.LocalDateTime
import io.github.serpro69.kfaker.Faker
import org.example.LocationCoordinates
import org.example.ParkingLocation
import kotlin.random.Random
import java.time.format.DateTimeFormatter


object FakeDataGenerator {
    private val faker = Faker()
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    val now = LocalDateTime.now().format(formatter)

    fun generateFakeParkingLocation(): ParkingLocation {
        val longitude = Random.nextDouble(13.0, 17.0)
        val latitude = Random.nextDouble(45.5, 47.0)

        return ParkingLocation(
            name = faker.company.name(),
            address = faker.address.fullAddress(),
            location = LocationCoordinates(
                coordinates = listOf(longitude, latitude)
            ),
            total_regular_spots = (10..100).random(),
            total_invalid_spots = (0..10).random(),
            total_bus_spots = (0..5).random(),
            available_regular_spots = (0..50).random(),
            available_invalid_spots = (0..5).random(),
            available_bus_spots = (0..3).random(),
            created = LocalDateTime.now(),
            modified = LocalDateTime.now(),
            description = faker.lorem.words(),
            hidden = false
        )
    }

    fun generateFakeUser(): UserUpload {
        val plainPassword = faker.random.randomString(5)
        return UserUpload(
            username = faker.name.firstName().lowercase() + Random.nextInt(100, 999),
            atribut = faker.job.title(),
            email = faker.internet.safeEmail(),
            password = plainPassword,
            password_hash = plainPassword,
            phone_number = "386" + (30..70).random().toString() + (1000000..9999999).random().toString(),
            credit_card_number = faker.finance.creditCard("visa"),
            created_at = LocalDateTime.now().format(formatter),
            updated_at = LocalDateTime.now().format(formatter),
            user_type = if ((0..1).random() == 0) "user" else "admin",
            hidden = false
        )
    }

    fun generateFakeVehicle(): VehicleUpload {
        val vehicleTypes = listOf("car", "truck", "motorcycle", "bus", "van")
        val letters = ('A'..'Z').toList()
        val numbers = (1000..9999).random()

        val registration = buildString {
            append(letters.random())
            append(letters.random())
            append(numbers)
            append(letters.random())
            append(letters.random())
        }



        return VehicleUpload(
            registration_number = registration,
            vehicle_type = vehicleTypes.random(),
            created = now,
            modified = now,
            user = "placeholderUserId", // ← Use dummy value for now
            hidden = false,
            isValid = true
        )
    }
    fun generateFakeReview(): ReviewUpload {
        val faker = Faker()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val now = LocalDateTime.now().format(formatter)
        val testUserId = "000000000000000000000001"
        val testParkingLocationId = "000000000000000000000002"

        return ReviewUpload(
            rating = (1..5).random(),
            review_text = faker.lorem.words(),
            review_date = now,
            created = now,
            modified = now,
            hidden = false,
            user = testUserId,
            parking_location = testParkingLocationId
        )
    }

}
