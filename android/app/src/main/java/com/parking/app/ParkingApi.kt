package com.parking.app

import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

/** The Spring Boot REST API (see README.md). */
interface ParkingApi {

    @GET("parking-areas")
    suspend fun areas(): List<ParkingArea>

    @GET("parking-areas/{id}")
    suspend fun area(@Path("id") id: Int): ParkingArea

    @POST("reservations")
    suspend fun reserve(@Body request: ReservationRequest): Reservation

    @GET("reservations")
    suspend fun reservations(@Query("userId") userId: String): List<Reservation>

    @GET("reservations/{id}")
    suspend fun reservation(@Path("id") id: Int): Reservation

    @POST("reservations/{id}/exit")
    suspend fun exit(@Path("id") id: Int): Reservation

    @POST("reservations/{id}/issues")
    suspend fun reportIssue(@Path("id") id: Int, @Body request: IssueRequest): Unit

    companion object {
        fun create(baseUrl: String = BuildConfig.API_URL): ParkingApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
            .build()
            .create(ParkingApi::class.java)
    }
}

/** The backend sends {"message": "..."} with its errors (server.error.include-message=always). Reads the error body, so call it once. */
fun Throwable.userMessage(): String = when (this) {
    is HttpException -> runCatching {
        json.parseToJsonElement(response()!!.errorBody()!!.string()).jsonObject["message"]!!.jsonPrimitive.content
    }.getOrDefault("Server error (${code()})")
    is IOException -> "Cannot reach the server at ${BuildConfig.API_URL}"
    else -> message ?: "Something went wrong"
}
