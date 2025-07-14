package org.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.gson.*

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.javatime.datetime
import java.util.UUID

// ✅ 1. Define your Users table
object Users : Table("users") {
    val id = uuid("id").autoGenerate().uniqueIndex()
    val email = text("email").uniqueIndex()
    val passwordHash = text("password_hash")
    val createdAt = datetime("created_at") // Use datetime here
    val userType = text("user_type")
}

// ✅ 2. Main function starts Ktor and connects to DB
fun main() {
    // ✅ Connect to local PostgreSQL (make sure your Docker container is running)
    Database.connect(
        url = "jdbc:postgresql://localhost:5432/ezymapdb",
        driver = "org.postgresql.Driver",
        user = "postgres",
        password = "password"
    )
    
    // ✅ Create tables
    transaction {
        SchemaUtils.create(Users)
    }
    
    // ✅ Start Ktor server
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson()
        }
        
        routing {
            get("/") {
                call.respondText("EzyMap Backend API is running!")
            }
        }
    }.start(wait = true)
}
