package com.example.data.remote

import retrofit2.http.*

/**
 * Supabase PostgREST API Service interface.
 * Endpoints correspond to the PostgreSQL tables defined in supabase_schema.sql.
 * Supports task management, proof submission, wallet operations, and admin actions.
 */
interface SupabaseApiService {

    @GET("profiles")
    suspend fun getProfile(
        @Query("phone") phoneFilter: String? = null,
        @Query("id") idFilter: String? = null
    ): List<Map<String, Any>>

    @POST("profiles")
    suspend fun upsertProfile(@Body profile: Map<String, Any>): List<Map<String, Any>>

    @GET("businesses")
    suspend fun getBusinesses(
        @Query("verification_status") status: String? = "eq.APPROVED"
    ): List<Map<String, Any>>

    @POST("businesses")
    suspend fun createBusiness(@Body business: Map<String, Any>): List<Map<String, Any>>

    @PATCH("businesses")
    suspend fun updateBusiness(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any>
    ): List<Map<String, Any>>

    @GET("campaigns")
    suspend fun getCampaigns(
        @Query("status") status: String? = "eq.ACTIVE"
    ): List<Map<String, Any>>

    @POST("campaigns")
    suspend fun createCampaign(@Body campaign: Map<String, Any>): List<Map<String, Any>>

    @PATCH("campaigns")
    suspend fun updateCampaign(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any>
    ): List<Map<String, Any>>

    @GET("tasks")
    suspend fun getTasks(
        @Query("user_id") userId: String? = null,
        @Query("status") status: String? = null
    ): List<Map<String, Any>>

    @POST("tasks")
    suspend fun createTask(@Body task: Map<String, Any>): List<Map<String, Any>>

    @PATCH("tasks")
    suspend fun updateTask(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any>
    ): List<Map<String, Any>>

    @POST("feedback")
    suspend fun submitFeedback(@Body feedback: Map<String, Any>): List<Map<String, Any>>

    @GET("feedback")
    suspend fun getFeedback(
        @Query("task_id") taskId: String? = null
    ): List<Map<String, Any>>

    @GET("wallets")
    suspend fun getWallet(
        @Query("user_id") userId: String
    ): List<Map<String, Any>>

    @POST("wallets")
    suspend fun upsertWallet(@Body wallet: Map<String, Any>): List<Map<String, Any>>

    @PATCH("wallets")
    suspend fun updateWallet(
        @Query("user_id") userId: String,
        @Body updates: Map<String, Any>
    ): List<Map<String, Any>>

    @POST("wallet_transactions")
    suspend fun createTransaction(@Body tx: Map<String, Any>): List<Map<String, Any>>

    @GET("wallet_transactions")
    suspend fun getTransactions(
        @Query("user_id") userId: String
    ): List<Map<String, Any>>

    @POST("withdrawals")
    suspend fun requestWithdrawal(@Body withdrawal: Map<String, Any>): List<Map<String, Any>>

    @PATCH("withdrawals")
    suspend fun updateWithdrawal(
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any>
    ): List<Map<String, Any>>

    @GET("settings")
    suspend fun getSettings(): List<Map<String, Any>>
}
