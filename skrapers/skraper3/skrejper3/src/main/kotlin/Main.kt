/*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

@Serializable
data class Tariff(
    val tariffZone: String,
    val workingHours: String,
    val dailyTariff: String,
    val nightTariff: String,
    val unifiedTariff: String,
    val dailyPriceWithVAT: String,
    val dailyUnit: String,
    val nightPriceWithVAT: String,
    val nightUnit: String,
    val unifiedPriceWithVAT: String,
    val unifiedUnit: String
)

@Serializable
data class LocationWithTariffs(
    val location: String,
    val tariffs: List<Tariff>
)

fun main() {
    val filePath = "C:/Users/CHP/Desktop/FERI II/2. letnik/2.semestar/PROJEKAT PARKINGMATE/sve ostalo/skrejper3/parkingCene.csv"
    val lines = File(filePath).readLines()

    val dataLines = lines.drop(1)
    val grouped = mutableListOf<LocationWithTariffs>()
    var currentLocation = ""
    var currentTariffs = mutableListOf<Tariff>()

    for (line in dataLines) {
        val parts = line.split(",")

        if (parts.size < 12) continue

        val locationField = parts[0].trim()
        val location = if (locationField.isNotEmpty()) locationField else currentLocation

        val tariff = Tariff(
            tariffZone = parts[1].trim(),
            workingHours = parts[2].trim(),
            dailyTariff = parts[3].trim(),
            nightTariff = parts[4].trim(),
            unifiedTariff = parts[5].trim(),
            dailyPriceWithVAT = parts[6].trim(),
            dailyUnit = parts[7].trim(),
            nightPriceWithVAT = parts[8].trim(),
            nightUnit = parts[9].trim(),
            unifiedPriceWithVAT = parts[10].trim(),
            unifiedUnit = parts[11].trim()
        )

        if (locationField.isNotEmpty()) {
            if (currentLocation.isNotEmpty() && currentTariffs.isNotEmpty()) {
                grouped.add(LocationWithTariffs(currentLocation, currentTariffs.toList()))
            }
            currentLocation = location
            currentTariffs = mutableListOf(tariff)
        } else {
            currentTariffs.add(tariff)
        }
    }

    if (currentLocation.isNotEmpty() && currentTariffs.isNotEmpty()) {
        grouped.add(LocationWithTariffs(currentLocation, currentTariffs.toList()))
    }

    val outputPath = "C:/Users/CHP/Desktop/FERI II/2. letnik/2.semestar/PROJEKAT PARKINGMATE/sve ostalo/skrejper3/parkingCeneGrupisano.json"
    val jsonOutput = Json { prettyPrint = true }.encodeToString(grouped)
    File(outputPath).writeText(jsonOutput)

}*/
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.File

@Serializable
data class Tariff(
    val tariffZone: String,
    val workingHours: String,
    val dailyTariff: String,
    val nightTariff: String,
    val unifiedTariff: String,
    val dailyPriceWithVAT: String,
    val dailyUnit: String,
    val nightPriceWithVAT: String,
    val nightUnit: String,
    val unifiedPriceWithVAT: String,
    val unifiedUnit: String
)

@Serializable
data class LocationWithTariffs(
    val location: String,
    val tariffs: List<Tariff>
)

@Serializable
data class ParkingTariffInfo(
    val location: String,
    val tariffZone: String,
    val workingHours: String,
    val dailyTariff: String,
    val nightTariff: String,
    val unifiedTariff: String,
    val dailyPrice: String,
    val dailyUnit: String,
    val nightPrice: String,
    val nightUnit: String,
    val unifiedPrice: String,
    val unifiedUnit: String
)

fun main() {
    val inputPath = "C:/Users/CHP/Desktop/FERI II/2. letnik/2.semestar/PROJEKAT PARKINGMATE/sve ostalo/skrejper3/parkingCeneGrupisano.json"
    val jsonString = File(inputPath).readText()

    val json = Json { ignoreUnknownKeys = true }
    val locations = json.decodeFromString<List<LocationWithTariffs>>(jsonString)

    val flatTariffs = locations.flatMap { location ->
        location.tariffs.map { tariff ->
            ParkingTariffInfo(
                location = location.location,
                tariffZone = tariff.tariffZone,
                workingHours = tariff.workingHours,
                dailyTariff = tariff.dailyTariff,
                nightTariff = tariff.nightTariff,
                unifiedTariff = tariff.unifiedTariff,
                dailyPrice = tariff.dailyPriceWithVAT,
                dailyUnit = tariff.dailyUnit,
                nightPrice = tariff.nightPriceWithVAT,
                nightUnit = tariff.nightUnit,
                unifiedPrice = tariff.unifiedPriceWithVAT,
                unifiedUnit = tariff.unifiedUnit
            )
        }
    }

    flatTariffs.forEachIndexed { index, info ->
        println("${index + 1}. ${Json.encodeToString(info)}")
    }
}
