package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Supabase client configuration for ReviewTask.
 * Uses PostgREST endpoints and Row-Level Security headers (apiKey, Authorization).
 */
object SupabaseConfig {
    private fun resolveUrl(): String {
        val bc = BuildConfig.SUPABASE_URL
        if (bc.isNotBlank() && !bc.contains("your-project.supabase.co")) return bc
        val env = System.getenv("SUPABASE_URL")
        if (!env.isNullOrBlank() && !env.contains("your-project.supabase.co")) return env
        return "https://twvlwktosyvmsvjmmoyz.supabase.co"
    }

    private fun resolveAnonKey(): String {
        val bc = BuildConfig.SUPABASE_ANON_KEY
        if (bc.isNotBlank() && !bc.contains("your-anon-key-here")) return bc
        val env = System.getenv("SUPABASE_ANON_KEY")
        if (!env.isNullOrBlank() && !env.contains("your-anon-key-here")) return env
        return "sb_publishable_doruh97fb8n3dFYj8aUTGw_R7dO8jhd"
    }

    var supabaseUrl: String = resolveUrl()
    var supabaseAnonKey: String = resolveAnonKey()
    var userAccessToken: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("apikey", supabaseAnonKey)
            .header("Content-Type", "application/json")
            .header("Prefer", "return=representation")

        val token = userAccessToken ?: supabaseAnonKey
        builder.header("Authorization", "Bearer $token")

        chain.proceed(builder.build())
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun getRetrofit(baseUrl: String = supabaseUrl): Retrofit {
        val safeUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl("${safeUrl}rest/v1/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: SupabaseApiService
        get() = getRetrofit(supabaseUrl).create(SupabaseApiService::class.java)

    /**
     * Verifies the connection to the configured Supabase project.
     * Uses the GoTrue /auth/v1/health endpoint which authenticates with the publishable/anon key.
     */
    suspend fun verifyConnection(
        testUrl: String = supabaseUrl,
        testKey: String = supabaseAnonKey
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = testUrl.trim()
            val key = testKey.trim()
            if (url.isBlank() || key.isBlank()) {
                return@withContext Result.failure(IllegalStateException("Supabase URL or Anon Key is empty."))
            }

            val safeUrl = if (url.endsWith("/")) url else "$url/"
            val healthUrl = "${safeUrl}auth/v1/health"

            val request = Request.Builder()
                .url(healthUrl)
                .header("apikey", key)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    Result.success("Connection successful (HTTP ${response.code}): $body")
                } else {
                    Result.failure(Exception("Supabase returned HTTP ${response.code}: ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
