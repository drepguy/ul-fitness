package com.example.ul_fitness.routes

import com.example.ul_fitness.db.Users
import com.example.ul_fitness.security.JwtConfig
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.time.LocalDateTime

@Serializable data class RegisterRequest(val email: String, val password: String)
@Serializable data class LoginRequest(val email: String, val password: String)
@Serializable data class AuthResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long)

fun Route.authRoutes() {
    val allowRegister = System.getenv("ALLOW_REGISTER")?.toBooleanStrictOrNull() ?: false

    post("/api/v1/auth/register") {
        if (!allowRegister) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "registration disabled"))
            return@post
        }
        val body = call.receiveText()
        val req = try { kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<RegisterRequest>(body) } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid json: ${e.message}")); return@post }
        if (req.email.isBlank() || req.password.length < 6) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "email/password invalid"))
            return@post
        }
        val exists = transaction { Users.selectAll().where { Users.email eq req.email }.count() > 0 }
        if (exists) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "email exists"))
            return@post
        }
        val hash = BCrypt.hashpw(req.password, BCrypt.gensalt())
        val id = transaction {
            Users.insert { it[email] = req.email; it[passwordHash] = hash; it[createdAt] = LocalDateTime.now() }[Users.id]
        }
        call.respond(HttpStatusCode.Created, mapOf("id" to id))
    }

    post("/api/v1/auth/login") {
        val bodyLogin = call.receiveText()
        val req = try { kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<LoginRequest>(bodyLogin) } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid json: ${e.message}")); return@post }
        val pair = transaction {
            val r = Users.selectAll().where { Users.email eq req.email }.singleOrNull() ?: return@transaction null
            Pair(r[Users.id], r[Users.passwordHash])
        } ?: run { call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials")); return@post }
        val (uid, hash) = pair
        if (!BCrypt.checkpw(req.password, hash)) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
            return@post
        }
        val access = JwtConfig.generateAccessToken(uid, req.email)
        val refresh = JwtConfig.generateRefreshToken(uid)
        call.respond(AuthResponse(access, refresh, JwtConfig.accessExpiresSec))
    }

    post("/api/v1/auth/refresh") {
        val bodyStr = call.receiveText()
        val body = try { kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<Map<String, String>>(bodyStr) } catch (e: Exception) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid json")); return@post }
        val token = body["refreshToken"] ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "missing refreshToken")); return@post }
        try {
            val decoded = JwtConfig.verifier.verify(token)
            if (decoded.getClaim("type").asString() != "refresh") throw Exception("not refresh")
            val uid = decoded.getClaim("uid").asLong()
            val email = transaction { Users.selectAll().where { Users.id eq uid }.singleOrNull()?.get(Users.email) } ?: "user"
            val access = JwtConfig.generateAccessToken(uid, email)
            call.respond(mapOf("accessToken" to access, "expiresIn" to JwtConfig.accessExpiresSec))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid refresh"))
        }
    }
}
