package com.example.ul_fitness.routes

import com.example.ul_fitness.db.Gyms
import com.example.ul_fitness.db.Workouts
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

@Serializable data class GymDto(val id: Long?, val name: String, val city: String? = null, val isSystem: Boolean = false, val ownerId: Long? = null)
@Serializable data class CreateGymRequest(val name: String, val city: String? = null)

fun Route.gymRoutes() {
    authenticate("auth-jwt") {
        get("/api/v1/gyms") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val rows = transaction {
                Gyms.selectAll().where { (Gyms.ownerId.isNull() and Gyms.isSystem.eq(true)) or (Gyms.ownerId eq uid) }
                    .map { GymDto(it[Gyms.id], it[Gyms.name], it[Gyms.city], it[Gyms.isSystem], it[Gyms.ownerId]) }
            }
            call.respond(rows)
        }
        post("/api/v1/gyms") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val req = call.receive<CreateGymRequest>()
            if (req.name.isBlank()) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "name required")); return@post }
            val exists = transaction { Gyms.selectAll().where { (Gyms.ownerId eq uid) and (Gyms.name eq req.name) }.count() > 0 }
            if (exists) { call.respond(HttpStatusCode.Conflict, mapOf("error" to "gym exists")); return@post }
            val id = transaction {
                Gyms.insert { it[ownerId] = uid; it[name] = req.name; it[city] = req.city; it[isSystem] = false; it[createdAt] = LocalDateTime.now() }[Gyms.id]
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }
        put("/api/v1/gyms/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@put }
            val req = call.receive<CreateGymRequest>()
            val updated = transaction { Gyms.update({ (Gyms.id eq id) and (Gyms.ownerId eq uid) }) { it[name] = req.name; it[city] = req.city } }
            if (updated == 0) call.respond(HttpStatusCode.NotFound) else call.respond(mapOf("ok" to true))
        }
        delete("/api/v1/gyms/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@delete }
            val deps = transaction { Workouts.selectAll().where { Workouts.gymId eq id }.count() > 0 }
            if (deps) { call.respond(HttpStatusCode.Conflict, mapOf("error" to "gym in use")); return@delete }
            val deleted = transaction { Gyms.deleteWhere { (Gyms.id eq id) and (Gyms.ownerId eq uid) } }
            if (deleted == 0) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.NoContent)
        }
    }
}