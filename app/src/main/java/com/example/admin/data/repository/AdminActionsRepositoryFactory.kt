package com.example.admin.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.network.RetrofitClient
import com.squareup.moshi.Moshi

/**
 * Factory responsible for constructing the correct [IAdminActionsRepository]
 * implementation based on the current build configuration.
 *
 * Resolution order (Dependency Inversion — consumers never reference impls directly):
 * 1. **Remote** — when [BuildConfig.ADMIN_API_BASE_URL] is configured, the
 *    [RemoteAdminActionsRepository] is used. This is the target production path.
 * 2. **Local** — DEBUG builds without a configured URL fall back to
 *    [LocalAdminActionsRepository] (SharedPreferences, no network required).
 * 3. **Firestore** — Release builds without a configured URL fall back to
 *    [FirestoreAdminActionsRepository] (existing cloud production path).
 */
object AdminActionsRepositoryFactory {

    /**
     * @param context Application context passed through to repository constructors.
     * @param moshi   Moshi instance shared from the application level.
     * @return The most appropriate [IAdminActionsRepository] for this build.
     */
    fun create(context: Context, moshi: Moshi): IAdminActionsRepository {
        val adminApiUrl = BuildConfig.ADMIN_API_BASE_URL
        val isRemoteConfigured = adminApiUrl.isNotBlank() &&
            !adminApiUrl.contains("placeholder", ignoreCase = true)

        return when {
            isRemoteConfigured -> RemoteAdminActionsRepository(
                context = context,
                adminApiService = RetrofitClient.adminApiService(context),
                authApiService = RetrofitClient.authApiService(context)
            )
            BuildConfig.DEBUG -> LocalAdminActionsRepository(context, moshi)
            else -> FirestoreAdminActionsRepository()
        }
    }
}
