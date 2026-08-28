package com.example.ul_fitness.db

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Gyms : Table("gyms") {
    val id = long("id").autoIncrement()
    val ownerId = long("owner_id").nullable()
    val name = varchar("name", 120)
    val city = varchar("city", 120).nullable()
    val isSystem = bool("is_system")
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Exercises : Table("exercises") {
    val id = long("id").autoIncrement()
    val ownerId = long("owner_id").nullable()
    val gymId = long("gym_id").nullable()
    val name = varchar("name", 120)
    val category = varchar("category", 20)
    val kind = varchar("kind", 20)
    val iconKey = varchar("icon_key", 40)
    val isSystem = bool("is_system")
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object ExerciseAliases : Table("exercise_aliases") {
    val id = long("id").autoIncrement()
    val exerciseId = long("exercise_id").references(Exercises.id)
    val alias = varchar("alias", 120)
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object Workouts : Table("workouts") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id)
    val gymId = long("gym_id").nullable()
    val startedAt = datetime("started_at")
    val endedAt = datetime("ended_at").nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object WorkoutExercises : Table("workout_exercises") {
    val id = long("id").autoIncrement()
    val workoutId = long("workout_id").references(Workouts.id)
    val exerciseId = long("exercise_id").references(Exercises.id)
    val orderIdx = integer("order_idx")
    override val primaryKey = PrimaryKey(id)
}

object Sets : Table("sets") {
    val id = long("id").autoIncrement()
    val workoutExerciseId = long("workout_exercise_id").references(WorkoutExercises.id)
    val setNo = integer("set_no")
    val reps = integer("reps")
    val weightKg = decimal("weight_kg", 5, 2)
    val isWarmup = bool("is_warmup")
    val rpe = integer("rpe").nullable()
    val isFailure = bool("is_failure")
    val note = text("note").nullable()
    val createdAt = datetime("created_at")
    override val primaryKey = PrimaryKey(id)
}

object WorkoutTemplates : Table("workout_templates") {
    val id = long("id").autoIncrement()
    val userId = long("user_id").references(Users.id)
    val gymId = long("gym_id").references(Gyms.id)
    val name = varchar("name", 120)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    override val primaryKey = PrimaryKey(id)
}

object WorkoutTemplateExercises : Table("workout_template_exercises") {
    val id = long("id").autoIncrement()
    val templateId = long("template_id").references(WorkoutTemplates.id)
    val exerciseId = long("exercise_id").references(Exercises.id)
    val orderIdx = integer("order_idx")
    val defaultSets = integer("default_sets")
    val defaultReps = integer("default_reps").nullable()
    val defaultWeightKg = decimal("default_weight_kg", 5, 2).nullable()
    override val primaryKey = PrimaryKey(id)
}
