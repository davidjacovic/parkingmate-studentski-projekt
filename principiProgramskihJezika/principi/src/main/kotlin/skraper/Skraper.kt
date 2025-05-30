package skraper

import java.io.File
import it.skrape.core.htmlDocument
import it.skrape.fetcher.skrape
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.selects.html5.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.example.ParkingLocation


//podatkovana struktura za dnevne podatke parkirisca
@Serializable
data class Dnevni(
    val stanje: String,
    val prosta: String?,
    val naVoljo: String?
)

//podatkovana struktura za abonente parkirisca
@Serializable
data class Abonenti(
    val naVoljo: String?,
    val oddana: String?,
    val prosta: String?,
    val cakalnaVrsta: String?
)

//glavna podatkovana struktura za informacije o prakiriscu
@Serializable
data class ParkirisceInfo(
    val parkirisce: String,
    val dnevni: Dnevni?,
    val abonenti: Abonenti?
)

//podatkovana struktura za tarife parkirisca
@Serializable
data class ParkirisceTarifa(
    val tarifa: String,
    val tip: String,
    val cas: String,
    val enotaMere: String,
    val cena: String
)

//podatkovana struktura za lokacijo parkirisca z dodatnimi podatki
@Serializable
data class ParkirisceLokacijaPodaci(
    val lokacija: String,
    val opis: String = "",
    val tarife: List<ParkirisceTarifa>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val stMestBus: Int? = null,
    val stMestInv: Int? = null,

    )

//podatkovana struktura za GeoJson
data class GeoJsonFeature(
    val name: String,
    val stMestBus: Int,
    val stMestInv: Int
)

//funkcija za nalaganje GeoJSON podatkov iz datoteke
fun ucitajGeoJsonPutanju(geoJsonFilePath: String): Map<String, GeoJsonFeature> {
    val geoJsonString = File(geoJsonFilePath).readText()
    val json = Json.parseToJsonElement(geoJsonString).jsonObject
    val features = json["features"]?.jsonArray ?: return emptyMap()

    return features.mapNotNull { feature ->
        val props = feature.jsonObject["properties"]?.jsonObject ?: return@mapNotNull null
        val name = props["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
        val stMestBus = props["StMestBus"]?.jsonPrimitive?.intOrNull ?: 0
        val stMestInv = props["StMestInv"]?.jsonPrimitive?.intOrNull ?: 0

        name.trim().lowercase() to GeoJsonFeature(name, stMestBus, stMestInv)
    }.toMap()
}

//funkcija za skraper podatkov z dolocene URL lokacije
fun skraper(url: String, geoJsonPodaci: Map<String, GeoJsonFeature>): ParkirisceLokacijaPodaci? {

    var rezultat: ParkirisceLokacijaPodaci? = null
    skrape(HttpFetcher) {
        request { this.url = url }

        extract {
            htmlDocument {
                val firstH1 = h1 { findFirst { text } }
                val normalizedName = firstH1.lowercase()
                val geo = geoJsonPodaci[normalizedName]
                val blocks = findAll("div.block-item.block-text")

                var opisText = ""
                blocks.forEach { block ->
                    val heading = block.findFirst("h2")?.text ?: ""
                    if (heading == "Opis") {
                        val paragraphs = block.findAll("p").joinToString("\n") { it.text.trim() }
                        opisText = paragraphs
                    }
                }

                val tarife: List<ParkirisceTarifa>? = try {
                    val cenaSection = findAll("div.prose")
                        .firstOrNull { it.findFirst("h2")?.text?.contains("Cena parkiranja", ignoreCase = true) == true }

                    if (cenaSection != null) {
                        val listaTarifa = mutableListOf<ParkirisceTarifa>()
                        cenaSection.findAll("table").forEach { tabela ->
                            val theadRows = tabela.findAll("thead tr")
                            val tarifaIme = theadRows.firstOrNull()?.findFirst("th")?.text?.trim() ?: "Neznana tarifa"
                            val dataRows = tabela.findAll("tbody tr")
                            dataRows.forEach { row ->
                                val cells = row.findAll("td, th").map { it.text.trim().replace("&euro;", "€") }
                                if (cells.size >= 4) {
                                    listaTarifa.add(
                                        ParkirisceTarifa(
                                            tarifa = tarifaIme,
                                            tip = cells[0],
                                            cas = cells[1],
                                            enotaMere = cells[2],
                                            cena = cells[3]
                                        )
                                    )
                                }
                            }
                        }
                        if (listaTarifa.isNotEmpty()) listaTarifa else null
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    null
                }

                var lat: Double? = null
                var lng: Double? = null
                val googleMapsLinkElement = try {
                    findAll("a[title='Odpri lokacijo v Google Maps']").firstOrNull()
                } catch (e: Exception) {
                    null
                }
                val googleMapsLink = googleMapsLinkElement?.attribute("href")
                if (!googleMapsLink.isNullOrBlank()) {
                    val regex = Regex("""destination=([0-9.]+),([0-9.]+)""")
                    val match = regex.find(googleMapsLink)
                    if (match != null) {
                        val latStr = match.groupValues[1]
                        val lngStr = match.groupValues[2]
                        if (latStr.isNotBlank() && lngStr.isNotBlank()) {
                            lat = latStr.toDoubleOrNull()
                            lng = lngStr.toDoubleOrNull()
                        }
                    }
                }

                rezultat = ParkirisceLokacijaPodaci(
                    lokacija = firstH1,
                    opis = opisText,
                    tarife = tarife,
                    latitude = lat,
                    longitude = lng,
                    stMestBus = geo?.stMestBus,
                    stMestInv = geo?.stMestInv
                )
            }
        }
    }
    return rezultat
}
fun ucitajParkingLokacijeJson(): List<JsonObject> {
    val geoJsonMapa = ucitajGeoJsonPutanju("C:/Users/CHP/Desktop/git/parkingmate-studentski-projekt/skrape/skraper2/podaciSaStranice/molparkirisca/output.geojson")

    val parkiriscaZasedenost = mutableMapOf<String, ParkirisceInfo>()
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/informacije-za-parkiranje/prikaz-zasedenosti-parkirisc"
        }
        extract {
            htmlDocument {
                table {
                    findFirst("tbody") {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("td, th").map { it.text.trim() }

                            if (cells.isNotEmpty()) {
                                val ime = cells[0]
                                val key = ime.lowercase().trim()
                                val abonentiInfo = cells.getOrNull(2)?.split(" ") ?: listOf()
                                val dnevniRaw = cells.getOrNull(1) ?: ""

                                val dnevni = run {
                                    val parts = dnevniRaw.trim().split(Regex("\\s+"))
                                    val brojniDelovi = parts.filter { it.matches(Regex("""[\d/]+""")) }
                                    when (brojniDelovi.size) {
                                        2 -> Dnevni(
                                            stanje = parts.dropLast(2).joinToString(" "),
                                            prosta = brojniDelovi[0],
                                            naVoljo = brojniDelovi[1]
                                        )
                                        1 -> Dnevni(
                                            stanje = parts.dropLast(1).joinToString(" "),
                                            prosta = brojniDelovi[0],
                                            naVoljo = null
                                        )
                                        else -> Dnevni(
                                            stanje = dnevniRaw,
                                            prosta = null,
                                            naVoljo = null
                                        )
                                    }
                                }

                                val abonenti = if (abonentiInfo.isNotEmpty()) {
                                    Abonenti(
                                        naVoljo = abonentiInfo.getOrNull(0),
                                        oddana = abonentiInfo.getOrNull(1),
                                        prosta = abonentiInfo.getOrNull(2),
                                        cakalnaVrsta = abonentiInfo.getOrNull(3)
                                    )
                                } else null

                                parkiriscaZasedenost[key] = ParkirisceInfo(
                                    parkirisce = ime,
                                    dnevni = dnevni,
                                    abonenti = abonenti
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val koncnaLista = mutableListOf<JsonObject>()
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/informacije-za-parkiranje/prikaz-zasedenosti-parkirisc"
        }
        extract {
            htmlDocument {
                table {
                    findFirst("tbody") {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("td, th").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                val ime = cells[0]
                                val seoName = ime.lowercase()
                                    .replace("č", "c").replace("š", "s").replace("ž", "z")
                                    .replace("đ", "dj").replace("ć", "c")
                                    .replace("p+r", "pr")
                                    .replace(Regex("[^a-z0-9]+"), "-")
                                    .trim('-')

                                val fullUrl = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/$seoName"

                                try {
                                    val lokacija = skraper(fullUrl, geoJsonMapa)
                                    if (lokacija != null) {
                                        val zasedenost = parkiriscaZasedenost[ime.lowercase().trim()]
                                        val obj = buildJsonObject {
                                            put("lokacija", lokacija.lokacija)
                                            put("opis", lokacija.opis)
                                            put("location", if (lokacija.latitude != null && lokacija.longitude != null) {
                                                buildJsonObject {
                                                    put("type", "Point")
                                                    put("coordinates", JsonArray(listOf(
                                                        JsonPrimitive(lokacija.longitude),
                                                        JsonPrimitive(lokacija.latitude)
                                                    )))
                                                }
                                            } else {
                                                JsonNull
                                            })
                                            put("stMestBus", lokacija.stMestBus ?: 0)
                                            put("stMestInv", lokacija.stMestInv ?: 0)

                                            lokacija.tarife?.let { tarifeList ->
                                                put("tarife", JsonArray(tarifeList.map {
                                                    buildJsonObject {
                                                        put("tarifa", it.tarifa)
                                                        put("tip", it.tip)
                                                        put("cas", it.cas)
                                                        put("enotaMere", it.enotaMere)
                                                        put("cena", it.cena)
                                                    }
                                                }))
                                            }
                                            zasedenost?.dnevni?.let {
                                                put("dnevni", Json.encodeToJsonElement(it))
                                            }
                                            zasedenost?.abonenti?.let {
                                                put("abonenti", Json.encodeToJsonElement(it))
                                            }
                                        }
                                        koncnaLista.add(obj)
                                    }
                                } catch (e: Exception) {
                                    println("Napaka za $fullUrl: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    return koncnaLista
}

fun main() {
    val jsonObjekti = ucitajParkingLokacijeJson()

    println(Json { prettyPrint = true }.encodeToString(JsonArray(jsonObjekti)))



// sad imaš listu ParkingLocation objekata spremnu za UI

    //DEO ZA PROVERU DA LI SKENER RADI
    /*val outputFile = File("C:/Users/CHP/Desktop/git/parkingmate-studentski-projekt/skrape/finalSkraper/parking.json")
    outputFile.writeText(Json { prettyPrint = true }.encodeToString(JsonArray(koncnaLista)))
    println("Podatki uspešno zapisani.")*/
}

