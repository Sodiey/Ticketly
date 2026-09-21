package com.example.ticketly.network

import com.example.ticketly.network.dto.ErrorEnvelopeDto
import java.io.IOException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val errorBodyJson = Json { ignoreUnknownKeys = true }

/**
 * Runs a Retrofit suspend call and collapses both transport failures (no connection,
 * server unreachable) and the API's error envelope on a non-2xx response into a single
 * [Result.failure], so repositories - and the ViewModels reading their [Result]s - don't
 * need to tell the two apart.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    Result.failure(Exception(e.parseErrorMessage(), e))
} catch (e: IOException) {
    Result.failure(e)
}

private fun HttpException.parseErrorMessage(): String {
    val body = response()?.errorBody()?.string()
    val parsed = body?.let { runCatching { errorBodyJson.decodeFromString<ErrorEnvelopeDto>(it) }.getOrNull() }
    return parsed?.error?.text ?: message()
}
