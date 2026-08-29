package com.example.ul_fitness.routes

import com.example.ul_fitness.db.*
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable data class SetInput(val reps: Int, val weightKg: Double, val isWarmup: Boolean = false, val rpe: Int? = null, val isFailure: Boolean = false, val note: String? = null)
@Serializable data class WorkoutExerciseInput(val exerciseId: Long, val sets: List<SetInput>)
@Serializable data class CreateWorkoutRequest(val gymId: Long, val startedAt: String? = null, val notes: String? = null, val exercises: List<WorkoutExerciseInput>)
@Serializable data class WorkoutSummaryDto(val id: Long, val gymId: Long?, val gymName: String?, val startedAt: String, val endedAt: String?, val notes: String?)
@Serializable data class SetDto(val reps: Int, val weightKg: Double, val isWarmup: Boolean, val rpe: Int?, val isFailure: Boolean, val note: String?)
@Serializable data class WorkoutExerciseDto(val exerciseId: Long, val name: String, val iconKey: String, val sets: List<SetDto>)
@Serializable data class WorkoutDetailDto(val id: Long, val gymId: Long?, val startedAt: String, val endedAt: String?, val notes: String?, val exercises: List<WorkoutExerciseDto>)

fun Route.workoutRoutes() {
    authenticate("auth-jwt") {
        post("/api/v1/workouts") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val req = call.receive<CreateWorkoutRequest>()
            val gymExists = transaction { Gyms.selectAll().where { Gyms.id eq req.gymId }.count() > 0 }
            if (!gymExists) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "gym not found")); return@post }
            for (ex in req.exercises) {
                val row = transaction { Exercises.selectAll().where { Exercises.id eq ex.exerciseId }.singleOrNull() }
                    ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "exercise ${ex.exerciseId} not found")); return@post }
                val eg = row[Exercises.gymId]
                if (eg != null && eg != req.gymId) { call.respond(HttpStatusCode.Conflict, mapOf("error" to "machine belongs to other gym")); return@post }
                for (s in ex.sets) {
                    if (s.reps < 0) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "reps >=0")); return@post }
                    if (s.weightKg < 0) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "weight >=0")); return@post }
                    s.rpe?.let { if (it !in 1..10) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "rpe 1-10")); return@post } }
                }
            }
            val started = req.startedAt?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) } ?: LocalDateTime.now()
            val wid = transaction {
                val wId = Workouts.insert { it[userId] = uid; it[gymId] = req.gymId; it[startedAt] = started; it[notes] = req.notes; it[createdAt] = LocalDateTime.now() }[Workouts.id]
                req.exercises.forEachIndexed { idx, ex ->
                    val weId = WorkoutExercises.insert { it[workoutId] = wId; it[exerciseId] = ex.exerciseId; it[orderIdx] = idx }[WorkoutExercises.id]
                    ex.sets.forEachIndexed { sIdx, s ->
                        Sets.insert {
                            it[workoutExerciseId] = weId
                            it[setNo] = sIdx + 1
                            it[reps] = s.reps
                            it[weightKg] = BigDecimal(s.weightKg)
                            it[isWarmup] = s.isWarmup
                            it[rpe] = s.rpe
                            it[isFailure] = s.isFailure
                            it[note] = s.note
                            it[createdAt] = LocalDateTime.now()
                        }
                    }
                }
                wId
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to wid))
        }

        get("/api/v1/workouts") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1,100) ?: 20
            val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0
            val sinceStr = call.request.queryParameters["since"]
            val since = sinceStr?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
            val rows = transaction {
                val q = Workouts.selectAll().where { Workouts.userId eq uid }
                val q2 = if (gymId != null) q.andWhere { Workouts.gymId eq gymId } else q
                val q3 = if (since != null) q2.andWhere { Workouts.createdAt greater since } else q2
                q3.limit(limit, offset.toLong()).orderBy(Workouts.startedAt to SortOrder.DESC)
                    .map {
                        WorkoutSummaryDto(
                            id = it[Workouts.id],
                            gymId = it[Workouts.gymId],
                            gymName = it[Workouts.gymId]?.let { gid -> Gyms.selectAll().where { Gyms.id eq gid }.singleOrNull()?.get(Gyms.name) },
                            startedAt = it[Workouts.startedAt].toString(),
                            endedAt = it[Workouts.endedAt]?.toString(),
                            notes = it[Workouts.notes]
                        )
                    }
            }
            call.respond(rows)
        }

        get("/api/v1/workouts/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@get }
            val data = transaction {
                val w = Workouts.selectAll().where { (Workouts.id eq id) and (Workouts.userId eq uid) }.singleOrNull() ?: return@transaction null
                val exRows = WorkoutExercises.selectAll().where { WorkoutExercises.workoutId eq id }.orderBy(WorkoutExercises.orderIdx to SortOrder.ASC).map { we ->
                    val ex = Exercises.selectAll().where { Exercises.id eq we[WorkoutExercises.exerciseId] }.single()
                    val sets = Sets.selectAll().where { Sets.workoutExerciseId eq we[WorkoutExercises.id] }.orderBy(Sets.setNo to SortOrder.ASC).map {
                        SetDto(it[Sets.reps], it[Sets.weightKg].toDouble(), it[Sets.isWarmup], it[Sets.rpe], it[Sets.isFailure], it[Sets.note])
                    }
                    WorkoutExerciseDto(ex[Exercises.id], ex[Exercises.name], ex[Exercises.iconKey], sets)
                }
                WorkoutDetailDto(w[Workouts.id], w[Workouts.gymId], w[Workouts.startedAt].toString(), w[Workouts.endedAt]?.toString(), w[Workouts.notes], exRows)
            } ?: run { call.respond(HttpStatusCode.NotFound); return@get }
            call.respond(data)
        }

        patch("/api/v1/workouts/{id}/finish") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@patch }
            val body = call.receiveText().let { try { kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<Map<String,String>>(it) } catch (e: Exception) { emptyMap<String,String>() } }
            val ended = body["ended_at"]?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) } ?: body["endedAt"]?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) } ?: LocalDateTime.now()
            val updated = transaction { Workouts.update({ (Workouts.id eq id) and (Workouts.userId eq uid) }) { it[endedAt] = ended } }
            if (updated==0) call.respond(HttpStatusCode.NotFound) else call.respond(mapOf("ok" to true))
        }

        patch("/api/v1/workouts/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@patch }
            val body = call.receiveText().let { try { kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<Map<String,String>>(it) } catch (e: Exception) { emptyMap<String,String>() } }
            val gymId = body["gym_id"]?.toLongOrNull() ?: body["gymId"]?.toLongOrNull()
            val notes = body["notes"]
            if (gymId == null && notes == null) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "gym_id or notes required")); return@patch }
            if (gymId != null) {
                val exists = transaction { Gyms.selectAll().where { Gyms.id eq gymId }.count() > 0 }
                if (!exists) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "gym not found")); return@patch }
            }
            val updated = transaction {
                Workouts.update({ (Workouts.id eq id) and (Workouts.userId eq uid) }) {
                    if (gymId != null) it[Workouts.gymId] = gymId
                    if (notes != null) it[Workouts.notes] = notes
                }
            }
            if (updated==0) call.respond(HttpStatusCode.NotFound) else call.respond(mapOf("ok" to true))
        }

        delete("/api/v1/workouts/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@delete }
            val deleted = transaction { Workouts.deleteWhere { (Workouts.id eq id) and (Workouts.userId eq uid) } }
            if (deleted==0) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.NoContent)
        }

        post("/api/v1/workouts/from-last") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val body = call.receive<Map<String,Long>>()
            val gymId = body["gymId"] ?: run { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "gymId required")); return@post }
            val last = transaction { Workouts.selectAll().where { (Workouts.userId eq uid) and (Workouts.gymId eq gymId) }.orderBy(Workouts.startedAt to SortOrder.DESC).limit(1).singleOrNull() }
                ?: run { call.respond(HttpStatusCode.NotFound, mapOf("error" to "no previous")); return@post }
            val newId = transaction {
                val nid = Workouts.insert { it[userId] = uid; it[this.gymId] = gymId; it[startedAt] = LocalDateTime.now(); it[createdAt] = LocalDateTime.now() }[Workouts.id]
                val wes = WorkoutExercises.selectAll().where { WorkoutExercises.workoutId eq last[Workouts.id] }.orderBy(WorkoutExercises.orderIdx to SortOrder.ASC).toList()
                wes.forEach { we ->
                    WorkoutExercises.insert { it[workoutId] = nid; it[exerciseId] = we[WorkoutExercises.exerciseId]; it[orderIdx] = we[WorkoutExercises.orderIdx] }
                }
                nid
            }
            call.respond(HttpStatusCode.Created, mapOf("workoutId" to newId))
        }
    }
}