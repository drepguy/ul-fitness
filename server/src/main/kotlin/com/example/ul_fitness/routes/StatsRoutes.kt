package com.example.ul_fitness.routes

import com.example.ul_fitness.db.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable

@Serializable data class ProgressPointDto(val date: String, val e1RM: Double, val volume: Double, val maxWeight: Double, val reps: Int)
@Serializable data class AggregatedDayDto(val date: String, val e1RM: Double, val volume: Double, val maxWeight: Double, val totalSets: Int)
@Serializable data class PrDto(val maxWeight: Double, val maxWeightDate: String?, val maxE1RM: Double, val maxE1RMDate: String?, val maxVolume: Double, val maxVolumeDate: String?)
@Serializable data class DashboardStatsDto(val totalWorkouts: Int, val totalSets: Int, val totalVolume: Double, val workoutsPerWeek: Double, val exercisesTrained: Int, val periodDays: Int)
@Serializable data class MonthlyVolumeDto(val month: String, val volume: Double, val workouts: Int)

fun Route.statsRoutes() {
    authenticate("auth-jwt") {

        get("/api/v1/stats/progress") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val exerciseId = call.request.queryParameters["exerciseId"]?.toLongOrNull() ?: run { call.respond(mapOf("error" to "exerciseId required")); return@get }
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val fromStr = call.request.queryParameters["from"]
            val toStr = call.request.queryParameters["to"]
            val from = fromStr?.let { LocalDate.parse(it) }
            val to = toStr?.let { LocalDate.parse(it) }

            val points = transaction {
                val q = (Workouts innerJoin WorkoutExercises innerJoin Sets)
                    .selectAll().where {
                        (Workouts.userId eq uid) and
                        (WorkoutExercises.exerciseId eq exerciseId) and
                        (Sets.isWarmup eq false) and
                        (Sets.weightKg greater BigDecimal.ZERO)
                    }
                    .let { qq -> if (gymId != null) qq.andWhere { Workouts.gymId eq gymId } else qq }
                    .let { qq -> if (from != null) qq.andWhere { Workouts.startedAt greaterEq from.atStartOfDay() } else qq }
                    .let { qq -> if (to != null) qq.andWhere { Workouts.startedAt lessEq to.plusDays(1).atStartOfDay() } else qq }
                    .orderBy(Workouts.startedAt to SortOrder.ASC)
                    .map {
                        val reps = it[Sets.reps]
                        val weight = it[Sets.weightKg].toDouble()
                        val e1RM = weight * (1 + reps / 30.0)
                        val volume = reps * weight
                        ProgressPointDto(
                            date = it[Workouts.startedAt].format(DateTimeFormatter.ISO_DATE_TIME),
                            e1RM = e1RM,
                            volume = volume,
                            maxWeight = weight,
                            reps = reps
                        )
                    }
                q
            }
            call.respond(points)
        }

        get("/api/v1/stats/progress/daily") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val exerciseId = call.request.queryParameters["exerciseId"]?.toLongOrNull() ?: run { call.respond(mapOf("error" to "exerciseId required")); return@get }
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val fromStr = call.request.queryParameters["from"]
            val toStr = call.request.queryParameters["to"]
            val from = fromStr?.let { LocalDate.parse(it) }
            val to = toStr?.let { LocalDate.parse(it) }

            val days = transaction {
                val rows = (Workouts innerJoin WorkoutExercises innerJoin Sets)
                    .selectAll().where {
                        (Workouts.userId eq uid) and
                        (WorkoutExercises.exerciseId eq exerciseId) and
                        (Sets.isWarmup eq false) and
                        (Sets.weightKg greater BigDecimal.ZERO)
                    }
                    .let { qq -> if (gymId != null) qq.andWhere { Workouts.gymId eq gymId } else qq }
                    .let { qq -> if (from != null) qq.andWhere { Workouts.startedAt greaterEq from.atStartOfDay() } else qq }
                    .let { qq -> if (to != null) qq.andWhere { Workouts.startedAt lessEq to.plusDays(1).atStartOfDay() } else qq }
                    .orderBy(Workouts.startedAt to SortOrder.ASC)

                rows.groupBy { it[Workouts.startedAt].toLocalDate().toString() }
                    .map { (date, setRows) ->
                        val setMetrics = setRows.map {
                            val reps = it[Sets.reps]
                            val weight = it[Sets.weightKg].toDouble()
                            Triple(reps, weight, weight * (1 + reps / 30.0))
                        }
                        AggregatedDayDto(
                            date = date,
                            e1RM = setMetrics.maxOf { it.third },
                            volume = setMetrics.sumOf { it.first * it.second },
                            maxWeight = setMetrics.maxOf { it.second },
                            totalSets = setMetrics.size
                        )
                    }
            }
            call.respond(days)
        }

        get("/api/v1/stats/prs") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val exerciseId = call.request.queryParameters["exerciseId"]?.toLongOrNull() ?: run { call.respond(mapOf("error" to "exerciseId required")); return@get }
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val data = transaction {
                val rows = (Workouts innerJoin WorkoutExercises innerJoin Sets)
                    .selectAll().where {
                        (Workouts.userId eq uid) and
                        (WorkoutExercises.exerciseId eq exerciseId) and
                        (Sets.isWarmup eq false)
                    }
                    .let { qq -> if (gymId != null) qq.andWhere { Workouts.gymId eq gymId } else qq }
                    .map {
                        val reps = it[Sets.reps]
                        val weight = it[Sets.weightKg].toDouble()
                        val e1RM = weight * (1 + reps / 30.0)
                        val volume = reps * weight
                        val date = it[Workouts.startedAt].format(DateTimeFormatter.ISO_DATE_TIME)
                        Triple(weight, e1RM, volume) to date
                    }

                val maxWeight = rows.maxByOrNull { it.first.first }
                val maxE1RM = rows.maxByOrNull { it.first.second }
                val maxVolume = rows.maxByOrNull { it.first.third }

                PrDto(
                    maxWeight = maxWeight?.first?.first ?: 0.0,
                    maxWeightDate = maxWeight?.second,
                    maxE1RM = maxE1RM?.first?.second ?: 0.0,
                    maxE1RMDate = maxE1RM?.second,
                    maxVolume = maxVolume?.first?.third ?: 0.0,
                    maxVolumeDate = maxVolume?.second
                )
            }
            call.respond(data)
        }

        get("/api/v1/stats/dashboard") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val fromStr = call.request.queryParameters["from"]
            val toStr = call.request.queryParameters["to"]
            val from = fromStr?.let { LocalDate.parse(it) } ?: LocalDate.now().minusDays(90)
            val to = toStr?.let { LocalDate.parse(it) } ?: LocalDate.now()

            val stats = transaction {
                val q = Workouts.selectAll().where {
                    (Workouts.userId eq uid) and
                    (Workouts.startedAt greaterEq from.atStartOfDay()) and
                    (Workouts.startedAt lessEq to.plusDays(1).atStartOfDay())
                }.let { qq -> if (gymId != null) qq.andWhere { Workouts.gymId eq gymId } else qq }

                val workouts = q.toList()
                val workoutIds = workouts.map { it[Workouts.id] }.toSet()

                val setRows = if (workoutIds.isNotEmpty()) {
                    (WorkoutExercises innerJoin Sets)
                        .selectAll().where {
                            (WorkoutExercises.workoutId inList workoutIds) and
                            (Sets.isWarmup eq false)
                        }.toList()
                } else emptyList()

                val totalVolume = setRows.sumOf { it[Sets.reps] * it[Sets.weightKg].toDouble() }
                val exercisesTrained = setRows.map { it[WorkoutExercises.exerciseId] }.distinct().size
                val periodDays = java.time.temporal.ChronoUnit.DAYS.between(from, to).toInt().coerceAtLeast(1)
                val weeks = periodDays / 7.0

                DashboardStatsDto(
                    totalWorkouts = workouts.size,
                    totalSets = setRows.size,
                    totalVolume = totalVolume,
                    workoutsPerWeek = if (weeks > 0) workouts.size / weeks else 0.0,
                    exercisesTrained = exercisesTrained,
                    periodDays = periodDays
                )
            }
            call.respond(stats)
        }

        get("/api/v1/stats/monthly-volume") {
            val uid = call.principal<JWTPrincipal>()!!.payload.getClaim("uid").asLong()
            val gymId = call.request.queryParameters["gymId"]?.toLongOrNull()
            val fromStr = call.request.queryParameters["from"]
            val toStr = call.request.queryParameters["to"]
            val from = fromStr?.let { LocalDate.parse(it) } ?: LocalDate.now().minusDays(365)
            val to = toStr?.let { LocalDate.parse(it) } ?: LocalDate.now()

            val months = transaction {
                val workouts = Workouts.selectAll().where {
                    (Workouts.userId eq uid) and
                    (Workouts.startedAt greaterEq from.atStartOfDay()) and
                    (Workouts.startedAt lessEq to.plusDays(1).atStartOfDay())
                }.let { qq -> if (gymId != null) qq.andWhere { Workouts.gymId eq gymId } else qq }
                    .orderBy(Workouts.startedAt to SortOrder.ASC)
                    .toList()

                val workoutIds = workouts.map { it[Workouts.id] }.toSet()
                val setRows = if (workoutIds.isNotEmpty()) {
                    (Workouts innerJoin WorkoutExercises innerJoin Sets)
                        .selectAll().where {
                            (Workouts.id inList workoutIds) and (Sets.isWarmup eq false)
                        }.toList()
                } else emptyList()

                val grouped = workouts.groupBy { it[Workouts.startedAt].toLocalDate().withDayOfMonth(1).toString() }

                grouped.map { (month, wks) ->
                    val wIds = wks.map { it[Workouts.id] }.toSet()
                    val monthSets = setRows.filter { it[WorkoutExercises.workoutId] in wIds }
                    MonthlyVolumeDto(
                        month = month,
                        volume = monthSets.sumOf { it[Sets.reps] * it[Sets.weightKg].toDouble() },
                        workouts = wks.size
                    )
                }
            }
            call.respond(months)
        }
    }
}
