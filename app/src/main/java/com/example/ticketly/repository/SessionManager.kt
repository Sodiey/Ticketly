package com.example.ticketly.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current session's access token in memory only, mirroring the backend
 * itself (the token lives in server memory until logout, nothing is persisted).
 */
@Singleton
class SessionManager @Inject constructor() {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun setAccessToken(token: String) {
        _accessToken.value = token
    }

    fun clear() {
        _accessToken.value = null
    }
}
