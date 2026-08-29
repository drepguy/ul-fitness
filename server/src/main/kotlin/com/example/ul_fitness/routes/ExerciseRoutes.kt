package com.example.ul_fitness.routes

import com.example.ul_fitness.db.ExerciseAliases
import com.example.ul_fitness.db.Exercises
import com.example.ul_fitness.db.Gyms
import com.example.ul_fitness.db.Sets
import com.example.ul_fitness.db.WorkoutExercises
import com.example.ul_fitness.db.Workouts
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

private val json = Json { ignoreUnknownKeys = true }

@Serializable data class ExerciseDto(val id: Long?, val name: String, val category: String, val kind: String, val iconKey: String, val gymId: Long? = null, val gymName: String? = null, val isSystem: Boolean = false, val ownerId: Long? = null, val aliases: List<String> = emptyList())
@Serializable data class CreateExerciseRequest(val name: String, val category: String, val kind: String = "free_weight", val iconKey: String? = null, val gymId: Long? = null, val aliases: List<String> = emptyList())
@Serializable data class AliasUpdateRequest(val add: List<String> = emptyList(), val remove: List<String> = emptyList())
@Serializable data class LastSetDto(val reps: Int, val weightKg: Double, val rpe: Int? = null)

fun Route.exerciseRoutes() {
    authenticate("auth-jwt") {
        get("/api/v1/exercises") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val q = call.request.queryParameters["q"]?.trim()?.lowercase()
            val category = call.request.queryParameters["category"]
            val rows = transaction {
                val base = Exercises.selectAll().where { (Exercises.isSystem eq true) or (Exercises.ownerId eq uid) or (Exercises.ownerId.isNull()) }.toList()
                val filtered = base.filter { row ->
                    val g = row[Exercises.gymId]
                    when {
                        gymId == null -> true
                        g == null -> true
                        g == gymId -> true
                        else -> false
                    }
                }.filter { row -> if (category != null && row[Exercises.category] != category) false else true }
                 .filter { row ->
                    if (q.isNullOrBlank()) true else {
                        val name = row[Exercises.name].lowercase()
                        if (name.contains(q)) true else {
                            val eid = row[Exercises.id]
                            ExerciseAliases.selectAll().where { (ExerciseAliases.exerciseId eq eid) and (ExerciseAliases.alias.lowerCase() like "%$q%") }.count() > 0
                        }
                    }
                }.map { row ->
                    val eid = row[Exercises.id]
                    val gymName = row[Exercises.gymId]?.let { gid -> Gyms.selectAll().where { Gyms.id eq gid }.singleOrNull()?.get(Gyms.name) }
                    val aliases = ExerciseAliases.selectAll().where { ExerciseAliases.exerciseId eq eid }.map { it[ExerciseAliases.alias] }
                    ExerciseDto(eid, row[Exercises.name], row[Exercises.category], row[Exercises.kind], row[Exercises.iconKey], row[Exercises.gymId], gymName, row[Exercises.isSystem], row[Exercises.ownerId], aliases)
                }
                filtered
            }
            call.respond(rows)
        }

        post("/api/v1/exercises") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val text = call.receiveText()
            val req = try { json.decodeFromString<CreateExerciseRequest>(text) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid body: ${e.message}")); return@post
            }
            if (req.name.isBlank()) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "name required")); return@post }
            if (req.kind == "machine" && req.gymId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "machine needs gymId")); return@post }
            if (req.gymId != null) {
                val gymExists = transaction { Gyms.selectAll().where { Gyms.id eq req.gymId }.count() > 0 }
                if (!gymExists) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "gym not found")); return@post }
            }
            val dup = transaction { Exercises.selectAll().where { Exercises.name eq req.name }.any { it[Exercises.ownerId] == uid && it[Exercises.gymId] == req.gymId } }
            if (dup) { call.respond(HttpStatusCode.Conflict, mapOf("error" to "exists")); return@post }
            val icon = req.iconKey ?: "dumbbell"
            val id = transaction {
                Exercises.insert { it[ownerId] = uid; it[gymId] = req.gymId; it[name] = req.name; it[category] = req.category; it[kind] = req.kind; it[iconKey] = icon; it[isSystem] = false; it[createdAt] = LocalDateTime.now() }[Exercises.id]
            }
            transaction {
                req.aliases.forEach { alias ->
                    if (alias.isNotBlank()) ExerciseAliases.insert { it[exerciseId] = id; it[ExerciseAliases.alias] = alias; it[createdAt] = LocalDateTime.now() }
                }
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        put("/api/v1/exercises/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@put }
            val text = call.receiveText()
            val req = try { json.decodeFromString<CreateExerciseRequest>(text) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid body")); return@put
            }
            val updated = transaction {
                Exercises.update({ (Exercises.id eq id) and ((Exercises.ownerId eq uid) or Exercises.ownerId.isNull()) }) {
                    it[Exercises.name] = req.name
                    it[Exercises.category] = req.category
                    it[Exercises.kind] = req.kind
                    if (req.iconKey != null) it[Exercises.iconKey] = req.iconKey
                    it[Exercises.gymId] = req.gymId
                }
            }
            if (updated == 0) call.respond(HttpStatusCode.NotFound) else call.respond(mapOf("ok" to true))
        }

        get("/api/v1/exercises/{id}/aliases") {
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@get }
            val aliases = transaction { ExerciseAliases.selectAll().where { ExerciseAliases.exerciseId eq id }.map { it[ExerciseAliases.alias] } }
            call.respond(aliases)
        }

        put("/api/v1/exercises/{id}/aliases") {
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@put }
            val text = call.receiveText()
            val req = try { json.decodeFromString<AliasUpdateRequest>(text) } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid body")); return@put
            }
            transaction {
                req.remove.forEach { al -> ExerciseAliases.deleteWhere { (ExerciseAliases.exerciseId eq id) and (ExerciseAliases.alias eq al) } }
                req.add.forEach { al -> if (al.isNotBlank()) try { ExerciseAliases.insert { it[exerciseId] = id; it[alias] = al; it[createdAt] = LocalDateTime.now() } } catch (_: Exception) {} }
            }
            call.respond(mapOf("ok" to true))
        }

        delete("/api/v1/exercises/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@delete }
            val deleted = transaction { Exercises.deleteWhere { (Exercises.id eq id) and ((Exercises.ownerId eq uid) or Exercises.ownerId.isNull()) } }
            if (deleted == 0) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.NoContent)
        }

        get("/api/v1/exercises/{id}/last-sets") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val exerciseId = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@get }
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val data = transaction {
                val lastWorkout = Workouts.selectAll().where {
                    (Workouts.userId eq uid) and (Workouts.endedAt.isNotNull()) and
                    (gymId?.let { Workouts.gymId eq it } ?: Op.TRUE)
                }.orderBy(Workouts.startedAt to SortOrder.DESC).toList().firstOrNull { w ->
                    WorkoutExercises.selectAll().where { WorkoutExercises.workoutId eq w[Workouts.id] and (WorkoutExercises.exerciseId eq exerciseId) }.count() > 0
                } ?: return@transaction null
                val we = WorkoutExercises.selectAll().where {
                    WorkoutExercises.workoutId eq lastWorkout[Workouts.id] and (WorkoutExercises.exerciseId eq exerciseId)
                }.singleOrNull() ?: return@transaction null
                Sets.selectAll().where { Sets.workoutExerciseId eq we[WorkoutExercises.id] }
                    .orderBy(Sets.setNo to SortOrder.ASC).map {
                        LastSetDto(it[Sets.reps], it[Sets.weightKg].toDouble(), it[Sets.rpe])
                    }
            }
            call.respond(data ?: emptyList<Any>())
        }
    }
}
