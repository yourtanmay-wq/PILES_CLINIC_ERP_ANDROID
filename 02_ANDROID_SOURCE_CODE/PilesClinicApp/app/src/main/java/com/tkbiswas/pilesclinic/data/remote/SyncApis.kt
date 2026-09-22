package com.tkbiswas.pilesclinic.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * All four tables use the same PostgREST "upsert" trick for conflict-safe
 * sync: POST with `Prefer: resolution=merge-duplicates` and `on_conflict=id`
 * lets a single call safely insert a brand-new row OR update an existing one
 * with the same id, without a separate "does it exist?" round trip.
 */

interface EnquiryApi {
    @GET("rest/v1/enquiries")
    suspend fun getUpdatedSince(
        @Query("updated_at") updatedAtFilter: String,
        @Query("select") select: String = "*"
    ): List<EnquiryDto>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/enquiries?on_conflict=id")
    suspend fun upsert(@Body items: List<EnquiryDto>): List<EnquiryDto>
}

interface RegistrationApi {
    @GET("rest/v1/registrations")
    suspend fun getUpdatedSince(
        @Query("updated_at") updatedAtFilter: String,
        @Query("select") select: String = "*"
    ): List<RegistrationDto>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/registrations?on_conflict=id")
    suspend fun upsert(@Body items: List<RegistrationDto>): List<RegistrationDto>
}

interface FollowUpApi {
    @GET("rest/v1/follow_ups")
    suspend fun getUpdatedSince(
        @Query("updated_at") updatedAtFilter: String,
        @Query("select") select: String = "*"
    ): List<FollowUpDto>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/follow_ups?on_conflict=id")
    suspend fun upsert(@Body items: List<FollowUpDto>): List<FollowUpDto>
}

interface PaymentApi {
    @GET("rest/v1/payments")
    suspend fun getUpdatedSince(
        @Query("updated_at") updatedAtFilter: String,
        @Query("select") select: String = "*"
    ): List<PaymentDto>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/payments?on_conflict=id")
    suspend fun upsert(@Body items: List<PaymentDto>): List<PaymentDto>
}
