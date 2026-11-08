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
data class ReservationRequest(val spotId: Int, val userId: String)

@Serializable
data class IssueRequest(val type: IssueType, val description: String)

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
