package com.parking.app

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.Serializable
import okhttp3.MediaType
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException

/** The Spring Boot REST API (see schema.sql). */
interface ParkingApi {

    @GET("parking-areas")
    suspend fun areas(): List<ParkingArea>

    @GET("parking-areas/{id}")
    suspend fun area(@Path("id") id: Int): ParkingArea

    @POST("reservations")
    suspend fun reserve(@Body request: ReservationRequest): Reservation

    @GET("reservations")
    suspend fun reservations(@Query("userId") userId: Int): List<Reservation>

    @GET("reservations/{id}")
    suspend fun reservation(@Path("id") id: Int): Reservation

    @POST("reservations/{id}/exit")
    suspend fun exit(@Path("id") id: Int): Reservation

    @POST("reservations/{id}/issues")
    suspend fun reportIssue(@Path("id") id: Int, @Body request: IssueRequest): Unit

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): UserAccount

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserAccount

    @GET("auth/csrf")
    suspend fun csrf(): CsrfResponse

    /** The logged-in account as the server knows it now (e.g. banned since the last login). */
    @GET("auth/me")
    suspend fun me(): UserAccount

    @POST("auth/ban-status")
    suspend fun banStatus(@Body request: BanStatusRequest): BanInfo

    @POST("auth/appeal")
    suspend fun appeal(@Body request: AppealRequest): BanInfo

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int): UserAccount

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: Int,
        @Header("X-XSRF-TOKEN") csrfToken: String,
        @Body request: UpdateUserRequest,
    ): UserAccount

    // SCRUM-88: ticket counter (TICKET_COUNTER or ADMIN role only).
    @GET("counter/reservations")
    suspend fun counterReservations(): List<CounterReservation>

    @GET("counter/non-paying-reports")
    suspend fun nonPayingReports(): List<NonPayingReport>

    @POST("counter/non-paying-reports")
    suspend fun reportNonPaying(
        @Header("X-XSRF-TOKEN") csrfToken: String,
        @Body request: NonPayingReportRequest,
    ): NonPayingReport

    companion object {
        fun create(context: Context, baseUrl: String = BuildConfig.API_URL): ParkingApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(OkHttpClient.Builder().cookieJar(SessionCookieJar.get(context)).build())
            .addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
            .build()
            .create(ParkingApi::class.java)

        /** Forgets the server session (JSESSIONID) and CSRF cookie, e.g. on logout. */
        fun clearSession(context: Context) = SessionCookieJar.get(context).clear()
    }
}

@Serializable
data class CsrfResponse(val token: String)

/**
 * Keeps the Spring session cookie (JSESSIONID) and the XSRF-TOKEN cookie.
 * Cookies are also written to SharedPreferences so the server session survives
 * an app restart (the logged-in user is restored from SharedPreferences too).
 */
private class SessionCookieJar private constructor(private val prefs: SharedPreferences) : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    init {
        prefs.getStringSet(KEY, emptySet())!!.forEach { stored ->
            val (origin, header) = stored.split('\n', limit = 2).takeIf { it.size == 2 } ?: return@forEach
            val url = HttpUrl.parse(origin) ?: return@forEach
            Cookie.parse(url, header)?.let { cookies.add(it) }
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            this.cookies.removeAll {
                it.name() == cookie.name() && it.domain() == cookie.domain() && it.path() == cookie.path()
            }
            if (cookie.expiresAt() > System.currentTimeMillis()) {
                this.cookies.add(cookie)
            }
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        if (cookies.removeAll { it.expiresAt() <= now }) persist()
        return cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        persist()
    }

    private fun persist() {
        val stored = cookies.map { cookie ->
            val scheme = if (cookie.secure()) "https" else "http"
            "$scheme://${cookie.domain()}${cookie.path()}\n$cookie"
        }.toSet()
        prefs.edit().putStringSet(KEY, stored).apply()
    }

    companion object {
        private const val KEY = "cookies"
        @Volatile private var instance: SessionCookieJar? = null

        fun get(context: Context): SessionCookieJar = instance ?: synchronized(this) {
            instance ?: SessionCookieJar(
                context.applicationContext.getSharedPreferences("parking_session", Context.MODE_PRIVATE),
            ).also { instance = it }
        }
    }
}

/** Login answers 403 with the ban details for a banned account: returns them, or null for any other error body. */
fun parseBanInfo(errorBody: String): BanInfo? =
    runCatching { json.decodeFromString<BanInfo>(errorBody) }.getOrNull()?.takeIf { it.banned }

/** The {"message": "..."} of an error body already read. */
fun errorMessageOf(errorBody: String): String? =
    runCatching { json.parseToJsonElement(errorBody).jsonObject["message"]!!.jsonPrimitive.content }.getOrNull()

/** The backend sends {"message": "..."} with its errors (server.error.include-message=always). Reads the error body, so call it once. */
fun Throwable.userMessage(): String = when (this) {
    is HttpException -> runCatching {
        json.parseToJsonElement(response()!!.errorBody()!!.string()).jsonObject["message"]!!.jsonPrimitive.content
    }.getOrDefault("Server error (${code()})")
    is IOException -> "Cannot reach the server at ${BuildConfig.API_URL}"
    else -> message ?: "Something went wrong"
}
