package com.example.ul_fitness

import kotlinx.serialization.Serializable

@Serializable
data class Gym(
    val id: Long? = null,
    val name: String,
    val city: String? = null,
    val isSystem: Boolean = false
)

@Serializable
data class Exercise(
    val id: Long? = null,
    val name: String,
    val category: String, // push/pull/legs/core/...
    val kind: String, // free_weight/machine/cable/bodyweight/other
    val iconKey: String = "dumbbell",
    val gymId: Long? = null,
    val isSystem: Boolean = false,
    val aliases: List<String> = emptyList()
)
