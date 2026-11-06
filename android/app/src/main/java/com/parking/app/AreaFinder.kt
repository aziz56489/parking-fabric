package com.parking.app

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

/** A parking area with its parsed plan and its distance from the user (null when unknown). */
data class AreaInfo(val area: ParkingArea, val layout: Layout?, val distanceMeters: Double?) {
    val freeSpots: Int get() = area.spots.count { it.status == SpotStatus.AVAILABLE }
    val location: GeoPoint? get() = layout?.location

    companion object {
        fun of(area: ParkingArea, user: GeoPoint?): AreaInfo {
            val layout = area.parseLayout()
            val location = layout?.location
            val distance = if (user != null && location != null) distanceMeters(user, location) else null
            return AreaInfo(area, layout, distance)
        }
    }
}

/** Straight-line distance on the Earth's surface (haversine formula). */
fun distanceMeters(from: GeoPoint, to: GeoPoint): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val a = sin(dLat / 2).pow(2) +
        cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sin(dLng / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a))
}

/** Nearest first; areas without coordinates (or without a user location) go last, by name. */
fun rankAreas(areas: List<ParkingArea>, user: GeoPoint?): List<AreaInfo> =
    areas.map { AreaInfo.of(it, user) }
        .sortedWith(compareBy<AreaInfo, Double?>(nullsLast()) { it.distanceMeters }.thenBy { it.area.name })

/** The nearest area that still has at least one available spot. */
fun closestVacant(areas: List<AreaInfo>): AreaInfo? =
    areas.filter { it.freeSpots > 0 && it.distanceMeters != null }.minByOrNull { it.distanceMeters!! }
