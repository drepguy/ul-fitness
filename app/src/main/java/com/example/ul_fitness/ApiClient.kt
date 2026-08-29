package com.example.ul_fitness

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "auth")
private val TOKEN_KEY = stringPreferencesKey("jwt_token")
private val REFRESH_KEY = stringPreferencesKey("jwt_refresh")

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class TokenResponse(val accessToken: String, val refreshToken: String, val expiresIn: Long)

@Serializable
data class GymDto(val id: Long?, val name: String, val city: String? = null, val isSystem: Boolean = false)

@Serializable
data class ExerciseDto(val id: Long?, val name: String, val category: String, val kind: String, val iconKey: String, val gymId: Long? = null, val gymName: String? = null, val isSystem: Boolean = false, val aliases: List<String> = emptyList())

@Serializable
data class WorkoutSummaryDto(val id: Long, val gymId: Long?, val gymName: String?, val startedAt: String, val endedAt: String?, val notes: String?)

@Serializable
data class CreateWorkoutRequest(val gymId: Long, val notes: String? = null, val exercises: List<WorkoutExerciseInput> = emptyList())

@Serializable
data class WorkoutExerciseInput(val exerciseId: Long, val sets: List<SetInput> = emptyList())

@Serializable
data class SetInput(val reps: Int, val weightKg: Double, val rpe: Int? = null, val note: String? = null)

@Serializable
data class SetDetailDto(val reps: Int, val weightKg: Double, val isWarmup: Boolean, val rpe: Int?, val isFailure: Boolean, val note: String?)

@Serializable
data class WorkoutExerciseDetailDto(val exerciseId: Long, val name: String, val iconKey: String, val sets: List<SetDetailDto>)

@Serializable
data class WorkoutDetailDto(val id: Long, val gymId: Long?, val gymName: String?, val startedAt: String, val endedAt: String?, val notes: String?, val exercises: List<WorkoutExerciseDetailDto>)

class ApiClient(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            level = LogLevel.HEADERS
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("ApiClient", message)
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
        defaultRequest {
            url("${BuildConfig.API_BASE_URL}/api/v1/")
        }
    }

    private var cachedToken: String? = null

    private suspend fun getToken(): String? {
        if (cachedToken != null) return cachedToken
        cachedToken = context.dataStore.data.map { it[TOKEN_KEY] }.first()
        return cachedToken
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response: TokenResponse = httpClient.post("auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }.body()
            cachedToken = response.accessToken
            context.dataStore.edit { prefs ->
                prefs[TOKEN_KEY] = response.accessToken
                prefs[REFRESH_KEY] = response.refreshToken
            }
            Result.success(response.accessToken)
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun logout() {
        cachedToken = null
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = getToken() != null

    private suspend inline fun <reified T> authedGet(path: String, noinline configure: HttpRequestBuilder.() -> Unit = {}): T? {
        val token = getToken() ?: return null
        return try {
            httpClient.get(path) {
                header("Authorization", "Bearer $token")
                configure()
            }.body()
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "GET $path failed", e)
            null
        }
    }

    private suspend inline fun <reified T, reified B> authedPost(path: String, body: B): T? {
        val token = getToken() ?: return null
        return try {
            httpClient.post(path) {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(body)
            }.body()
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "POST $path failed", e)
            null
        }
    }

    suspend fun getGyms(): List<GymDto> = authedGet("gyms") ?: emptyList()
    suspend fun getExercises(gymId: Long): List<ExerciseDto> = authedGet("exercises?gymId=$gymId") ?: emptyList()
    suspend fun getWorkouts(gymId: Long): List<WorkoutSummaryDto> = authedGet("workouts?gymId=$gymId") ?: emptyList()
    suspend fun getWorkoutDetail(id: Long): WorkoutDetailDto? {
        val token = getToken() ?: return null
        return try {
            val response = httpClient.get("workouts/$id") {
                header("Authorization", "Bearer $token")
            }
            android.util.Log.d("ApiClient", "getWorkoutDetail status=${response.status}")
            val body = response.bodyAsText()
            android.util.Log.d("ApiClient", "getWorkoutDetail body=$body")
            response.body()
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "getWorkoutDetail failed", e)
            null
        }
    }

    suspend fun updateWorkoutNotes(id: Long, notes: String): Boolean {
        val token = getToken() ?: return false
        return try {
            val response = httpClient.patch("workouts/$id") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(mapOf("notes" to notes))
            }
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "updateWorkoutNotes failed", e)
            false
        }
    }

    suspend fun deleteWorkout(id: Long): Boolean {
        val token = getToken() ?: return false
        return try {
            val response = httpClient.delete("workouts/$id") {
                header("Authorization", "Bearer $token")
            }
            response.status == HttpStatusCode.NoContent || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "deleteWorkout failed", e)
            false
        }
    }

    suspend fun createWorkout(req: CreateWorkoutRequest): Long? {
        @Serializable data class IdResponse(val id: Long)
        return try {
            val response = httpClient.post("workouts") {
                header("Authorization", "Bearer ${getToken()}")
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            android.util.Log.d("ApiClient", "createWorkout status=${response.status}")
            android.util.Log.d("ApiClient", "createWorkout body=${response.bodyAsText()}")
            response.body<IdResponse>().id
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "createWorkout failed", e)
            null
        }
    }

    suspend fun finishWorkout(id: Long): Boolean {
        val token = getToken() ?: return false
        return try {
            val response = httpClient.patch("workouts/$id/finish") {
                header("Authorization", "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(emptyMap<String, String>())
            }
            android.util.Log.d("ApiClient", "finishWorkout status=${response.status}")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            android.util.Log.e("ApiClient", "Finish workout failed", e)
            false
        }
    }
}
