package com.parking.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.sin

// Same JSON as the backend and the Angular editor (frontend/src/app/models.ts).

val json = Json { ignoreUnknownKeys = true }

@Serializable
enum class SpotType(val label: String) {
    REGULAR("Regular"),
    EV("EV"),
    HANDICAPPED("Handicapped"),
    PERSONNEL("Personnel"),
}

@Serializable
enum class SpotStatus(val label: String) {
    AVAILABLE("Available"),
    RESERVED("Reserved"),
    OCCUPIED("Occupied"),
    NOT_AVAILABLE("Not available"),
}

/** [layer] is the drawing order used by the editor: green space at the bottom, walls on top. */
@Serializable
enum class ItemKind(val layer: Int) {
    GREEN_SPACE(0),
    ROAD(1),
    ENTRANCE(2),
    EXIT(2),
    SPOT(3),
    PILLAR(4),
    WALL(4),
}

@Serializable
enum class IssueType(val label: String) {
    SPOT_TAKEN("Another vehicle is in my spot"),
    BLOCKED("The spot is blocked"),
    DAMAGED("The spot is damaged or unsafe"),
    OTHER("Other"),
}

@Serializable
data class ParkingSpot(val id: Int, val number: String, val type: SpotType, val status: SpotStatus)

@Serializable
data class ParkingArea(
    val id: Int,
    val name: String,
    val layout: String? = null,
    val capacity: Int = 0,
    val spots: List<ParkingSpot> = emptyList(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
)

/** One shape of the plan. Sizes are in metres; (x, y) is the top-left corner and the shape is rotated around it. */
@Serializable
data class Item(
    val kind: ItemKind,
    val type: SpotType? = null,
    val number: String? = null,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val angle: Double = 0.0,
)

/** The plan saved by the editor in ParkingArea.layout. latitude/longitude are the centre of the parking area. */
@Serializable
data class Layout(
    val width: Double = 80.0,
    val height: Double = 50.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val floors: List<List<Item>> = emptyList(),
)

@Serializable
data class Reservation(
    val id: Int,
    val spotId: Int,
    val spotNumber: String,
    val areaId: Int,
    val areaName: String,
    val startedAt: String,
    val endedAt: String? = null,
    val minutes: Long,
    val pricePerHour: Double,
    val amount: Double? = null,
    val currency: String,
    val active: Boolean,
)

@Serializable
data class ReservationRequest(val spotId: Int, val userId: Int)

@Serializable
data class IssueRequest(val type: IssueType, val description: String)

/** Matches the `users` table in schema.sql */
@Serializable
data class UserAccount(
    val id: Int,
    val name: String,
    val email: String,
    val role: String = "USER",
    val isHandicapped: Boolean = false,
    val isEvOwner: Boolean = false,
    val isBlocked: Boolean = false,
) {
    val isTicketCounter: Boolean get() = role == "TICKET_COUNTER" || role == "ADMIN"

    val statusLabel: String
        get() = when {
            isHandicapped -> "Handicapped"
            isEvOwner -> "EV Owner"
            role != "USER" -> role
            else -> "Regular"
        }
}

/** SCRUM-88: a reservation as the ticket counter sees it, with its driver. */
@Serializable
data class CounterReservation(
    val id: Int,
    val userId: Int? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val spotNumber: String,
    val areaName: String,
    val startedAt: String,
    val endedAt: String? = null,
    val minutes: Long,
    val amount: Double? = null,
    val currency: String,
    val active: Boolean,
    val reported: Boolean,
) {
    /** Only reservations of a registered user can be reported, once. */
    val canReport: Boolean get() = userId != null && userName != null && !reported
}

@Serializable
data class NonPayingReportRequest(val reservationId: Int, val note: String)

@Serializable
data class NonPayingReport(
    val id: Int,
    val reservationId: Int? = null,
    val userId: Int,
    val userName: String? = null,
    val userEmail: String? = null,
    val ticketCounterName: String? = null,
    val spotNumber: String? = null,
    val areaName: String? = null,
    val amount: Double? = null,
    val currency: String? = null,
    val note: String? = null,
    val reportedAt: String,
)

/** A banned user's appeal and the admin's answer. */
@Serializable
data class BanAppeal(
    val id: Int,
    val message: String,
    val status: String, // PENDING, ACCEPTED or REJECTED
    val createdAt: String,
    val adminResponse: String? = null,
    val resolvedAt: String? = null,
)

/** Why the admin banned the account, and the latest appeal. Also the body of login's 403 for a banned account. */
@Serializable
data class BanInfo(
    val banned: Boolean,
    val name: String? = null,
    val email: String? = null,
    val reason: String? = null,
    val bannedAt: String? = null,
    val appeal: BanAppeal? = null,
    val canAppeal: Boolean = false,
)

/** A banned user can't log in, so ban-status and appeal check the email and password. */
@Serializable
data class BanStatusRequest(val email: String, val password: String)

@Serializable
data class AppealRequest(val email: String, val password: String, val message: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val name: String, val email: String, val password: String)

@Serializable
data class UpdateUserRequest(
    val name: String,
    val email: String,
    val isHandicapped: Boolean,
    val isEvOwner: Boolean,
)

fun ParkingArea.parseLayout(): Layout? =
    layout?.let { runCatching { json.decodeFromString<Layout>(it) }.getOrNull() }

val Layout.location: GeoPoint?
    get() {
        val latVal = latitude ?: lat
        val lngVal = longitude ?: lng
        return if (latVal != null && lngVal != null) GeoPoint(latVal, lngVal) else null
    }

val ParkingArea.location: GeoPoint?
    get() {
        val latVal = latitude ?: lat
        val lngVal = longitude ?: lng
        return if (latVal != null && lngVal != null) GeoPoint(latVal, lngVal) else parseLayout()?.location
    }

fun floorName(index: Int) = "Level ${'A' + index}"

/** True if the point (in metres) is inside this rotated rectangle. */
fun Item.contains(pointX: Double, pointY: Double): Boolean {
    val radians = Math.toRadians(angle)
    val dx = pointX - x
    val dy = pointY - y
    val localX = dx * cos(radians) + dy * sin(radians)
    val localY = -dx * sin(radians) + dy * cos(radians)
    return localX in 0.0..width && localY in 0.0..height
}