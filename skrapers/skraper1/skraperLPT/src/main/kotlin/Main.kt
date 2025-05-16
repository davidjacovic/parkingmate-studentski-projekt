package org.example
import org.jsoup.Jsoup

import it.skrape.core.htmlDocument
import it.skrape.fetcher.skrape
import it.skrape.fetcher.HttpFetcher
import it.skrape.fetcher.extract
import it.skrape.selects.html5.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import kotlinx.serialization.json.*


@Serializable
data class Dnevni(
    val stanje: String,
    val prosta: String?,
    val naVoljo: String?
)

@Serializable
data class Abonenti(
    val naVoljo: String?,
    val oddana: String?,
    val prosta: String?,
    val cakalnaVrsta: String?
)

@Serializable
data class ParkirisceInfo(
    val parkirisce: String,
    val dnevni: Dnevni?,
    val abonenti: Abonenti?
)

@Serializable
data class ParkirisceInfoPodrobnosti(
    val tip: String,
    val cas: String,
    val enotaMere: String,
    val cena: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)


fun main() {
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
            }
        }
    }
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/bezigrad"
        }
        extract {
            htmlDocument {
                val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                val firstH1 = h1 { findFirst { text } }
                val tabele = findAll("table")

                // === KOORDINATE (link van tabele) ===
                val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                val href = linkElement.attribute("href")
                val (lat, lng) = href
                    ?.substringAfter("destination=")
                    ?.split(",")
                    ?.let { parts ->
                        val lat = parts.getOrNull(0)?.toDoubleOrNull()
                        val lng = parts.getOrNull(1)?.toDoubleOrNull()
                        lat to lng
                    } ?: (null to null)

                tabele.take(2).forEach { tabela ->
                    val vrstice = tabela.findAll("tr").drop(1)
                    vrstice.forEach { vrstica ->
                        val cells = vrstica.findAll("td, th").map { it.text.trim() }
                        if (cells.size >= 4) {
                            val tip = cells[0]
                            val cas = cells[1]
                            val enotaMere = cells[2]
                            val cena = cells[3]
                            parkirisca.add(
                                ParkirisceInfoPodrobnosti(
                                    tip = tip,
                                    cas = cas,
                                    enotaMere = enotaMere,
                                    cena = cena,
                                    latitude = lat,
                                    longitude = lng
                                )
                            )

                        }
                    }
                }
                println(firstH1 + Json.encodeToString(parkirisca))
            }
        }
    }

    skrape(HttpFetcher) {
        request {
            url =
                "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/dolenjska-cesta-strelisce"
        }
        extract {
            htmlDocument {
                val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                val firstH1 = h1 { findFirst { text } }
                val tabele = findAll("table")

                // === KOORDINATE (link van tabele) ===
                val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                val href = linkElement.attribute("href")
                val (lat, lng) = href
                    ?.substringAfter("destination=")
                    ?.split(",")
                    ?.let { parts ->
                        val lat = parts.getOrNull(0)?.toDoubleOrNull()
                        val lng = parts.getOrNull(1)?.toDoubleOrNull()
                        lat to lng
                    } ?: (null to null)

                tabele.take(2).forEach { tabela ->
                    val vrstice = tabela.findAll("tr").drop(1)
                    vrstice.forEach { vrstica ->
                        val cells = vrstica.findAll("td, th").map { it.text.trim() }
                        if (cells.size >= 4) {
                            val tip = cells[0]
                            val cas = cells[1]
                            val enotaMere = cells[2]
                            val cena = cells[3]
                            parkirisca.add(
                                ParkirisceInfoPodrobnosti(
                                    tip = tip,
                                    cas = cas,
                                    enotaMere = enotaMere,
                                    cena = cena,
                                    latitude = lat,
                                    longitude = lng
                                )
                            )
                        }
                    }
                }
                println(firstH1 + Json.encodeToString(parkirisca))
            }
        }
    }


        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/gospodarsko-razstavisce"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(3).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/ph-kongresni-trg"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)


                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/kozolec"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/kranjceva-ulica"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/linhartova"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(3).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }

        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/metelkova-ulica"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/mirje"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-barje"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-studenec"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pokopalisce-polje"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/povsetova-ulica"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/sanatorij-emona"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tivoli-i"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tivoli-ii"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/trg-mladinskih-delovnih-brigad"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/trg-prekomorskih-brigad"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-i"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-ii"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-iii"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-iv"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-v"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-jezica"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-stanezice"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tacen"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/bs4"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val linkElement = findFirst("a[title='Odpri lokacijo v Google Maps']")
                    val href = linkElement.attribute("href")
                    val (lat, lng) = href
                        ?.substringAfter("destination=")
                        ?.split(",")
                        ?.let { parts ->
                            val lat = parts.getOrNull(0)?.toDoubleOrNull()
                            val lng = parts.getOrNull(1)?.toDoubleOrNull()
                            lat to lng
                        } ?: (null to null)

                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/ph-rog"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val (lat, lng) = try {
                        val href = findFirst("a[title='Odpri lokacijo v Google Maps']").attribute("href")
                        href
                            ?.substringAfter("destination=")
                            ?.split(",")
                            ?.let { parts ->
                                val lat = parts.getOrNull(0)?.toDoubleOrNull()
                                val lng = parts.getOrNull(1)?.toDoubleOrNull()
                                lat to lng
                            } ?: (null to null)
                    } catch (e: Exception) {
                        null to null
                    }


                    tabele.take(2).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
        skrape(HttpFetcher) {
            request {
                url =
                    "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-letaliska"
            }
            extract {
                htmlDocument {
                    val parkirisca = mutableListOf<ParkirisceInfoPodrobnosti>()
                    val firstH1 = h1 { findFirst { text } }
                    val tabele = findAll("table")

                    // === KOORDINATE (link van tabele) ===
                    val (lat, lng) = try {
                        val href = findFirst("a[title='Odpri lokacijo v Google Maps']").attribute("href")
                        href
                            ?.substringAfter("destination=")
                            ?.split(",")
                            ?.let { parts ->
                                val lat = parts.getOrNull(0)?.toDoubleOrNull()
                                val lng = parts.getOrNull(1)?.toDoubleOrNull()
                                lat to lng
                            } ?: (null to null)
                    } catch (e: Exception) {
                        null to null
                    }


                    tabele.take(1).forEach { tabela ->
                        val vrstice = tabela.findAll("tr").drop(1)
                        vrstice.forEach { vrstica ->
                            val cells = vrstica.findAll("td, th").map { it.text.trim() }
                            if (cells.size >= 4) {
                                val tip = cells[0]
                                val cas = cells[1]
                                val enotaMere = cells[2]
                                val cena = cells[3]
                                parkirisca.add(
                                    ParkirisceInfoPodrobnosti(
                                        tip = tip,
                                        cas = cas,
                                        enotaMere = enotaMere,
                                        cena = cena,
                                        latitude = lat,
                                        longitude = lng
                                    )
                                )
                            }
                        }
                    }
                    println(firstH1 + Json.encodeToString(parkirisca))
                }
            }
        }
}


/*
fun main() {

    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/informacije-za-parkiranje/prikaz-zasedenosti-parkirisc"
        }
        extract {
            htmlDocument {
                // Grab only the first (main) data table
                val mainTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            // Print only non-empty rows
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }
    println()
    println("BEZIGRAD")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/bezigrad"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }
    println()
    println("DOLENJSKA CESTA STREZISCE")
    skrape(HttpFetcher) {
        request {
            url =
                "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/dolenjska-cesta-strelisce"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }
    println()
    println("GOSARJEVA ULICA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/gosarjeva-ulica"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }
    println()
    println("GOSPODARSKO RAZSTAVISCE")
    skrape(HttpFetcher) {
        request {
            url =
                "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/gospodarsko-razstavisce"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
                println()
                val thirdTable = table {
                    findThird {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }

        }
    }

    println()
    println("KOMANOVA ULICA")
    println()
    println("PH KONGRESNI TRG")
    skrape(HttpFetcher) {
        request {
            url =
                "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/ph-kongresni-trg"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("KOZOLEC")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/kozolec"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }
    println()
    println("KRANJCEVA ULICA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/kranjceva-ulica"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("LINHARTOVA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/linhartova"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
                println()
                val thirdTble = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }

        }

    }
    println()
    println("METELKOVA ULICA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/metelkova-ulica"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("MIRJE")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/mirje"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("P + R BARJE")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-barje"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("P + R BARJE")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-studenec"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("POKOPALISCE POLJE")
    skrape(HttpFetcher) {
        request {
            url =
                "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pokopalisce-polje"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("POVSETOVA ULICA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/povsetova-ulica"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("SANATORIJI EMONA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/sanatorij-emona"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("TIVOLI I")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tivoli-i"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("TIVOLI II")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tivoli-ii"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("TRG MLADINSKIH DELOVNIH BRIGAD")
    skrape(HttpFetcher) {
        request {
            url =
                "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/trg-mladinskih-delovnih-brigad"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }
    println()
    println("TRG PREKOMORSKIH BRIGAD")
    skrape(HttpFetcher) {
        request {
            url =
                "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/trg-prekomorskih-brigad"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }
                }
            }
        }
    }

    println()
    println("ZALE I")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-i"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("ZALE II")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-ii"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("ZALE III")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-iii"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("ZALE IV")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-iv"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("ZALE V")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/zale-v"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("P + R JEZICA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-jezica"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }
    println()
    println("P + R STANEZICE")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-stanezice"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("SRC STOZICE")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/src-stozice"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val thirdTable = table {
                    findThird {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("TACEN")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/tacen"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }


    println()
    println("BS4")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/bs4"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }
    println()
    println("PH ROG")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/ph-rog"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
                println()
                val secondTable = table {
                    findSecond {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }

    println()
    println("P + R LETALISCA")
    skrape(HttpFetcher) {
        request {
            url = "https://www.lpt.si/parkirisca/lokacije-in-opis-parkirisc/parkirisca-za-osebna-vozila/pr-letaliska"
        }
        extract {
            htmlDocument {
                val firstTable = table {
                    findFirst {
                        findAll("tr").forEach { row ->
                            val cells = row.findAll("th, td").map { it.text.trim() }
                            if (cells.isNotEmpty()) {
                                println(cells.joinToString(" | "))
                            }
                        }
                    }

                }
            }
        }
    }
}
*/