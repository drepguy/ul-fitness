package com.example.ul_fitness.routes

import com.example.ul_fitness.db.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.format.DateTimeFormatter

fun Route.statsRoutes() {
    authenticate("auth-jwt") {
        get("/api/v1/stats/progress") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val exerciseId = call.request.queryParameters["exerciseId"]?.toLongOrNull() ?: run { call.respond(mapOf("error" to "exerciseId required")); return@get }
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val points = transaction {
                val q = (Workouts innerJoin WorkoutExercises innerJoin Sets)
                    .selectAll().where { (Workouts.userId eq uid) and (WorkoutExercises.exerciseId eq exerciseId) and (Sets.isWarmup eq false) and (Sets.weightKg greater 0.toBigDecimal()) }
                    .let { qq -> if (gymId != null) qq.andWhere { Workouts.gymId eq gymId } else qq }
                    .orderBy(Workouts.startedAt to SortOrder.ASC)
                    .map {
                        val reps = it[Sets.reps]
                        val weight = it[Sets.weightKg].toDouble()
                        val e1RM = weight * (1 + reps / 30.0)
                        val volume = reps * weight
                        mapOf(
                            "date" to it[Workouts.startedAt].format(DateTimeFormatter.ISO_DATE_TIME),
                            "e1RM" to e1RM,
                            "volume" to volume,
                            "maxWeight" to weight,
                            "reps" to reps
                        )
                    }
                q
            }
            call.respond(points)
        }
        get("/api/v1/stats/prs") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val exerciseId = call.request.queryParameters["exerciseId"]?.toLongOrNull() ?: run { call.respond(mapOf("error" to "exerciseId required")); return@get }
            val data = transaction {
                val sets = (Workouts innerJoin WorkoutExercises innerJoin Sets)
                    .selectAll().where { (Workouts.userId eq uid) and (WorkoutExercises.exerciseId eq exerciseId) and (Sets.isWarmup eq false) }
                    .map { it[Sets.weightKg].toDouble() to it[Workouts.startedAt] }
                val maxW = sets.maxByOrNull { it.first }
                mapOf("maxWeight" to (maxW?.first ?: 0), "maxWeightDate" to maxW?.second?.toString())
            }
            call.respond(data)
        }
    }
}