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

@Serializable data class TemplateExerciseInput(val exerciseId: Long, val orderIdx: Int, val defaultSets: Int = 3, val defaultReps: Int? = null, val defaultWeightKg: Double? = null)
@Serializable data class CreateTemplateRequest(val gymId: Long, val name: String, val exercises: List<TemplateExerciseInput>)

fun Route.templateRoutes() {
    authenticate("auth-jwt") {
        get("/api/v1/templates") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val rows = transaction {
                val q = if (gymId != null) WorkoutTemplates.selectAll().where { (WorkoutTemplates.userId eq uid) and (WorkoutTemplates.gymId eq gymId) } else WorkoutTemplates.selectAll().where { WorkoutTemplates.userId eq uid }
                q.map { t ->
                    val exs = WorkoutTemplateExercises.selectAll().where { WorkoutTemplateExercises.templateId eq t[WorkoutTemplates.id] }.orderBy(WorkoutTemplateExercises.orderIdx to SortOrder.ASC).map {
                        val ex = Exercises.selectAll().where { Exercises.id eq it[WorkoutTemplateExercises.exerciseId] }.single()
                        mapOf("exerciseId" to ex[Exercises.id], "name" to ex[Exercises.name], "iconKey" to ex[Exercises.iconKey], "orderIdx" to it[WorkoutTemplateExercises.orderIdx])
                    }
                    mapOf("id" to t[WorkoutTemplates.id], "name" to t[WorkoutTemplates.name], "gymId" to t[WorkoutTemplates.gymId], "exercises" to exs)
                }
            }
            call.respond(rows)
        }
        post("/api/v1/templates") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val req = call.receive<CreateTemplateRequest>()
            val exists = transaction { WorkoutTemplates.selectAll().where { (WorkoutTemplates.userId eq uid) and (WorkoutTemplates.gymId eq req.gymId) and (WorkoutTemplates.name eq req.name) }.count() > 0 }
            if (exists) { call.respond(HttpStatusCode.Conflict); return@post }
            val id = transaction {
                val tid = WorkoutTemplates.insert { it[userId] = uid; it[gymId] = req.gymId; it[name] = req.name; it[createdAt] = LocalDateTime.now(); it[updatedAt] = LocalDateTime.now() }[WorkoutTemplates.id]
                req.exercises.forEach {
                    WorkoutTemplateExercises.insert { te -> te[templateId] = tid; te[exerciseId] = it.exerciseId; te[orderIdx] = it.orderIdx; te[defaultSets] = it.defaultSets; te[defaultReps] = it.defaultReps; te[defaultWeightKg] = it.defaultWeightKg?.let { w -> BigDecimal(w) } }
                }
                tid
            }
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }
        put("/api/v1/templates/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@put }
            val req = call.receive<CreateTemplateRequest>()
            transaction {
                WorkoutTemplates.update({ (WorkoutTemplates.id eq id) and (WorkoutTemplates.userId eq uid) }) { it[name] = req.name; it[gymId] = req.gymId; it[updatedAt] = LocalDateTime.now() }
                WorkoutTemplateExercises.deleteWhere { WorkoutTemplateExercises.templateId eq id }
                req.exercises.forEach {
                    WorkoutTemplateExercises.insert { te -> te[templateId] = id; te[exerciseId] = it.exerciseId; te[orderIdx] = it.orderIdx; te[defaultSets] = it.defaultSets; te[defaultReps] = it.defaultReps; te[defaultWeightKg] = it.defaultWeightKg?.let { w -> BigDecimal(w) } }
                }
            }
            call.respond(mapOf("ok" to true))
        }
        delete("/api/v1/templates/{id}") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@delete }
            val del = transaction { WorkoutTemplates.deleteWhere { (WorkoutTemplates.id eq id) and (WorkoutTemplates.userId eq uid) } }
            if (del==0) call.respond(HttpStatusCode.NotFound) else call.respond(HttpStatusCode.NoContent)
        }
        post("/api/v1/templates/{id}/start") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val id = call.parameters["id"]?.toLongOrNull() ?: run { call.respond(HttpStatusCode.BadRequest); return@post }
            val t = transaction { WorkoutTemplates.selectAll().where { (WorkoutTemplates.id eq id) and (WorkoutTemplates.userId eq uid) }.singleOrNull() } ?: run { call.respond(HttpStatusCode.NotFound); return@post }
            val wid = transaction {
                val nid = Workouts.insert { it[userId] = uid; it[gymId] = t[WorkoutTemplates.gymId]; it[startedAt] = LocalDateTime.now(); it[createdAt] = LocalDateTime.now() }[Workouts.id]
                val exs = WorkoutTemplateExercises.selectAll().where { WorkoutTemplateExercises.templateId eq id }.orderBy(WorkoutTemplateExercises.orderIdx to SortOrder.ASC).toList()
                exs.forEach { te -> WorkoutExercises.insert { it[workoutId] = nid; it[exerciseId] = te[WorkoutTemplateExercises.exerciseId]; it[orderIdx] = te[WorkoutTemplateExercises.orderIdx] } }
                nid
            }
            call.respond(HttpStatusCode.Created, mapOf("workoutId" to wid))
        }
    }
}