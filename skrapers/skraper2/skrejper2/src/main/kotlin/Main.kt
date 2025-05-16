import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class GeoJson(
    val type: String,
    val name: String,
    val features: List<Feature>
)

@Serializable
data class Feature(
    val type: String,
    val properties: Properties,
    val geometry: Geometry
)

@Serializable
data class Properties(
    val name: String,
    val StMest: Int,
    val StMestBus: Int,
    val StMestInv: Int
)

@Serializable
data class Geometry(
    val type: String,
    val coordinates: List<Double>
)

@Serializable
data class ParkingInfo(
    val name: String,
    val totalSpots: Int,
    val busSpots: Int,
    val invalidSpots: Int,
    val latitude: Double,
    val longitude: Double
)
fun main() {
    val jsonString = File("C:/Users/CHP/IdeaProjects/skrejper2/podaciSaStranice/molparkirisca/output.geojson").readText()

    val json = Json { ignoreUnknownKeys = true }
    val data = json.decodeFromString<GeoJson>(jsonString)

    val parkingList = data.features.map { feature ->
        val p = feature.properties
        val coords = feature.geometry.coordinates
        ParkingInfo(
            name = p.name,
            totalSpots = p.StMest,
            busSpots = p.StMestBus,
            invalidSpots = p.StMestInv,
            longitude = coords.getOrNull(0) ?: 0.0,
            latitude = coords.getOrNull(1) ?: 0.0
        )
    }

    parkingList.forEach { parking ->
        println(Json.encodeToString(parking))
    }
}
