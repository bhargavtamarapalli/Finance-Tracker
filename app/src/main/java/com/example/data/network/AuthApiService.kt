package com.example.data.network

import com.example.admin.data.network.FirebaseTokenRequestDto
import com.example.admin.data.network.AuthResponseDto
import com.example.admin.data.network.ApiResponseDto
import com.example.admin.data.network.RemoteUserDto

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface for the auth-service microservice.
 *
 * The login endpoint is intentionally unauthenticated (no JWT required)
 * because it is the endpoint that issues the JWT in the first place.
 *
 * Base URL is read from BuildConfig.AUTH_API_BASE_URL (injected from .env
 * via the Secrets Gradle Plugin — never hardcoded).
 */
interface AuthApiService {

    /**
     * Exchanges a Firebase ID token for a backend-issued JWT access token.
     *
     * This call provisions the user profile in PostgreSQL on first login
     * and syncs Firebase Custom Claims (admin role) on every subsequent call.
     *
     * @param body Contains the Firebase [idToken] obtained from [FirebaseAuth.currentUser].
     * @return [AuthResponseDto] containing the backend [accessToken] and user profile.
     */
    @POST("api/auth/login")
    suspend fun loginWithFirebaseToken(
        @Body body: FirebaseTokenRequestDto
    ): AuthResponseDto

    /**
     * Returns the profile of the currently authenticated user
     * using the JWT attached by [AuthTokenInterceptor].
     */
    @GET("api/auth/me")
    suspend fun getMe(): ApiResponseDto<RemoteUserDto>
}
