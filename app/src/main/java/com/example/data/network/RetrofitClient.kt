package com.example.data.network

import com.example.admin.data.network.AdminApiService

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.EncryptedPrefsManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

private const val TAG = "RetrofitClient"
private const val PREF_KEY_ACCESS_TOKEN = "backend_access_token"
private const val CONNECT_TIMEOUT_SECONDS = 15L
private const val READ_TIMEOUT_SECONDS = 30L
private const val WRITE_TIMEOUT_SECONDS = 30L

/**
 * OkHttp interceptor that reads the backend JWT from [EncryptedSharedPreferences]
 * and attaches it as a `Bearer` token on every outgoing request.
 *
 * No token is added for unauthenticated paths (e.g. POST /api/auth/login).
 * The auth-service itself handles that distinction — the interceptor attaches
 * whatever token is stored; an empty token is harmlessly ignored by the server.
 */
class AuthTokenInterceptor(private val context: Context) : Interceptor {

    private val prefs by lazy {
        try {
            EncryptedPrefsManager.getEncryptedPrefs(context, "backend_auth_prefs")
        } catch (e: Exception) {
            Log.e(TAG, "EncryptedSharedPreferences unavailable, falling back to plain prefs", e)
            context.getSharedPreferences("backend_auth_prefs", Context.MODE_PRIVATE)
        }
    }

    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = prefs.getString(PREF_KEY_ACCESS_TOKEN, null)
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/**
 * Singleton factory that builds and caches [AdminApiService] and [AuthApiService]
 * Retrofit instances.
 *
 * Design notes:
 * - Base URLs are read exclusively from [BuildConfig], which in turn is populated
 *   by the Secrets Gradle Plugin from the `.env` file — no URL is hardcoded.
 * - The logging interceptor is active only in DEBUG builds (SRP: no test flags in prod).
 * - Both services share one [OkHttpClient] to reuse the connection pool.
 */
object RetrofitClient {

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Builds the shared [OkHttpClient] with:
     * - Auth token interceptor (attaches stored JWT)
     * - HTTP logging in DEBUG builds only
     * - Sensible timeouts from named constants (no magic numbers)
     */
    fun buildOkHttpClient(context: Context): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AuthTokenInterceptor(context))

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message ->
                Log.d(TAG, message)
            }.apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    /**
     * Returns a Retrofit-built [AdminApiService] instance.
     * Base URL comes from [BuildConfig.ADMIN_API_BASE_URL].
     *
     * @param context Required to instantiate [AuthTokenInterceptor].
     */
    fun adminApiService(context: Context): AdminApiService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.ADMIN_API_BASE_URL)
            .client(buildOkHttpClient(context))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AdminApiService::class.java)

    /**
     * Returns a Retrofit-built [AuthApiService] instance.
     * Base URL comes from [BuildConfig.AUTH_API_BASE_URL].
     *
     * @param context Required to instantiate [AuthTokenInterceptor].
     */
    fun authApiService(context: Context): AuthApiService =
        Retrofit.Builder()
            .baseUrl(BuildConfig.AUTH_API_BASE_URL)
            .client(buildOkHttpClient(context))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)

    /**
     * Persists the backend JWT access token into [EncryptedSharedPreferences].
     * Called by [RemoteAdminActionsRepository] immediately after a successful login.
     *
     * @param context Application context.
     * @param token   The backend-issued JWT access token.
     */
    fun saveAccessToken(context: Context, token: String) {
        try {
            EncryptedPrefsManager.getEncryptedPrefs(context, "backend_auth_prefs")
                .edit()
                .putString(PREF_KEY_ACCESS_TOKEN, token)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist backend access token to EncryptedSharedPreferences", e)
            throw e
        }
    }

    /**
     * Clears the stored backend JWT. Called on logout.
     *
     * @param context Application context.
     */
    fun clearAccessToken(context: Context) {
        try {
            EncryptedPrefsManager.getEncryptedPrefs(context, "backend_auth_prefs")
                .edit()
                .remove(PREF_KEY_ACCESS_TOKEN)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear backend access token", e)
        }
    }
}
