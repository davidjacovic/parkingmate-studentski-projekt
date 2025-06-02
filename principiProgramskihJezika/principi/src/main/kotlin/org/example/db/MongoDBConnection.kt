package org.example.db

import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoDatabase
import org.bson.UuidRepresentation
import org.litote.kmongo.KMongo

/**
 * Singleton za upravljanje konekcijom prema MongoDB Atlas bazi.
 */
object MongoDBConnection {

    private const val CONNECTION_STRING =
        "mongodb+srv://keser2264:bsrD43gjpA1SWqRI@cluster0.s2z8giq.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0"

    private const val DATABASE_NAME = "test"

    // Kreiramo MongoClientSettings sa eksplicitnim uuidRepresentation
    private val settings = MongoClientSettings.builder()
        .applyConnectionString(com.mongodb.ConnectionString(CONNECTION_STRING))
        .uuidRepresentation(UuidRepresentation.STANDARD)  // ili LEGACY ako koristite staru verziju UUID-a u bazi
        .build()

    private val client: MongoClient = KMongo.createClient(settings)

    val database: MongoDatabase = client.getDatabase(DATABASE_NAME)

    init {
        println("✅ Povezano s MongoDB Atlas bazom: $DATABASE_NAME")
    }
}
