package org.example
import java.io.File
import it.skrape.core.htmlDocument
import it.skrape.fetcher.skrape
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.selects.html5.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import kotlinx.serialization.json.*

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
    val stMestInv: Int? = null
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

                //obravnava za opis in tarife
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

                //obravnava za koordinate
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

                //shranjevanje podatkov v rezultat
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


fun main() {
    val sveLokacije = mutableListOf<ParkirisceLokacijaPodaci>()

    //del skrapera sa zacetne strane
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/informacije-za-parkiranje/prikaz-zasedenosti-parkirisc"
        }
        extract {
            htmlDocument {
                val parkirisca = mutableListOf<ParkirisceInfo>()

                table {
                    findFirst {
                        findAll("tr").drop(1).forEach { row ->
                            val cells = row.findAll("td, th").map { it.text.trim() }

                            if (cells.isNotEmpty()) {
                                val ime = cells[0]
                                val dnevniInfo = cells.getOrNull(1)?.split(" ") ?: listOf()
                                val abonentiInfo = cells.getOrNull(2)?.split(" ") ?: listOf()

                                val dnevni = if (dnevniInfo.isNotEmpty()) {
                                    Dnevni(
                                        stanje = dnevniInfo.getOrNull(0) ?: "",
                                        prosta = dnevniInfo.getOrNull(1),
                                        naVoljo = dnevniInfo.getOrNull(2)
                                    )
                                } else null

                                val abonenti = if (abonentiInfo.isNotEmpty()) {
                                    Abonenti(
                                        naVoljo = abonentiInfo.getOrNull(0),
                                        oddana = abonentiInfo.getOrNull(1),
                                        prosta = abonentiInfo.getOrNull(2),
                                        cakalnaVrsta = abonentiInfo.getOrNull(3)
                                    )
                                } else null

                                parkirisca.add(
                                    ParkirisceInfo(
                                        parkirisce = ime,
                                        dnevni = dnevni,
                                        abonenti = abonenti
                                    )
                                )
                            }
                        }
                    }
                }
                val jsonString = Json.encodeToString(parkirisca)
                println(jsonString)
                val outputFile =
                    File("C:/Users/CHP/Desktop/FERI II/2. letnik/2.semestar/PROJEKAT PARKINGMATE/github/parkingmate-studentski-projekt/skrapers/skraper1/skraperLPT/zasedenost.json")
                outputFile.writeText(Json { prettyPrint = true }.encodeToString(parkirisca))


            }
        }
    }

    // del za vsako parkiralisce posebej
    val urls = listOf(
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/bezigrad",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/dolenjska-cesta-strelisce",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/gosarjeva-ulica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/gospodarsko-razstavisce",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/komanova-ulica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/ph-kongresni-trg",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/kozolec",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/kranjceva-ulica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/linhartova",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/metelkova-ulica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/mirje",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-barje",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-dolgi-most",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-studenec",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/ph-kolezija",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pokopalisce-polje",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/povsetova-ulica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/sanatorij-emona",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/slovenceva-ulica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tivoli-i",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tivoli-ii",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/trg-mladinskih-delovnih-brigad",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/trg-prekomorskih-brigad",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/stembalova-ulica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-i",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-ii",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-iii",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-iv",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-v",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-jezica",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-stanezice",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/src-stozice",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tacen",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-avtobuse/bratislavska",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/bs4",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/ph-rog",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-letaliska",
        "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/parkirisce-ph-ilirija",
    )

    val geoJsonMapa = ucitajGeoJsonPutanju("C:/Users/CHP/Desktop/FERI II/2. letnik/2.semestar/PROJEKAT PARKINGMATE/github/parkingmate-studentski-projekt/skrapers/skraper2/skrejper2/podaciSaStranice/molparkirisca/output.geojson")

    urls.forEach { url ->
        val podaci = skraper(url, geoJsonMapa)
        if (podaci != null) {
            sveLokacije.add(podaci)
        }
    }

    //Izpis v konzolo
    println(Json { prettyPrint = true }.encodeToString(sveLokacije))

    //Izpis v json file
    val outputFile = File("C:/Users/CHP/Desktop/FERI II/2. letnik/2.semestar/PROJEKAT PARKINGMATE/github/parkingmate-studentski-projekt/skrapers/skraper1/skraperLPT/parking.json")
    outputFile.writeText(Json { prettyPrint = true }.encodeToString(sveLokacije))

}
