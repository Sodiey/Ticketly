package com.example.ticketly.network

import com.example.ticketly.repository.SessionManager
import javax.inject.Inject
import okhttp3.Interceptor
import okhttp3.Response

/** Attaches `Authorization: Bearer <token>` to every request once a session exists. */
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.accessToken.value
        val request = chain.request().let {
            if (token == null) it else it.newBuilder().addHeader(X_ACCESS_TOKEN, "Bearer $token").build()
        }
        return chain.proceed(request)
    }
    companion object {
        private const val X_ACCESS_TOKEN: String = "Authorization"
    }
}
