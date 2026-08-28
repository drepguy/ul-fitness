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
                        mapOf(
                            "id" to it[Workouts.id],
                            "gymId" to it[Workouts.gymId],
                            "gymName" to it[Workouts.gymId]?.let { gid -> Gyms.selectAll().where { Gyms.id eq gid }.singleOrNull()?.get(Gyms.name) },
                            "startedAt" to it[Workouts.startedAt].toString(),
                            "endedAt" to it[Workouts.endedAt]?.toString(),
                            "notes" to it[Workouts.notes]
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
                        mapOf("reps" to it[Sets.reps], "weightKg" to it[Sets.weightKg], "isWarmup" to it[Sets.isWarmup], "rpe" to it[Sets.rpe], "isFailure" to it[Sets.isFailure], "note" to it[Sets.note])
                    }
                    mapOf("exerciseId" to ex[Exercises.id], "name" to ex[Exercises.name], "iconKey" to ex[Exercises.iconKey], "sets" to sets)
                }
                mapOf("id" to w[Workouts.id], "gymId" to w[Workouts.gymId], "startedAt" to w[Workouts.startedAt].toString(), "endedAt" to w[Workouts.endedAt]?.toString(), "notes" to w[Workouts.notes], "exercises" to exRows)
            } ?: run { call.respond(HttpStatusCode.NotFound); return@get }
            call.respond(data)
        }

        patch("/api/v1/workouts/{id}/finish") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@patch }
            val body = call.receive<Map<String,String>>()
            val ended = body["endedAt"]?.let { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) } ?: LocalDateTime.now()
            val updated = transaction { Workouts.update({ (Workouts.id eq id) and (Workouts.userId eq uid) }) { it[endedAt] = ended } }
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