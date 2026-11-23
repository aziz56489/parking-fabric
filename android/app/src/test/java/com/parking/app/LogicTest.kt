package com.parking.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class LogicTest {

    private fun area(id: Int, name: String, lat: Double?, lng: Double?, vararg statuses: SpotStatus): ParkingArea {
        val layout = if (lat == null || lng == null) null else """{"width":40,"height":20,"latitude":$lat,"longitude":$lng,"floors":[[]]}"""
        val spots = statuses.mapIndexed { i, status -> ParkingSpot(id * 100 + i, "A-0$i", SpotType.REGULAR, status) }
        return ParkingArea(id, name, layout, spots.size, spots)
    }

    @Test
    fun distanceBetweenTwoKnownPoints() {
        // Tunis centre to Carthage is about 14 km.
        val distance = distanceMeters(GeoPoint(36.8065, 10.1815), GeoPoint(36.8528, 10.3233))
        assertEquals(13_700.0, distance, 500.0)
        assertEquals(0.0, distanceMeters(GeoPoint(1.0, 2.0), GeoPoint(1.0, 2.0)), 0.001)
    }

    @Test
    fun closestVacantSkipsFullAreasAndAreasWithoutLocation() {
        val user = GeoPoint(36.80, 10.18)
        val ranked = rankAreas(
            listOf(
                area(1, "far", 36.90, 10.30, SpotStatus.AVAILABLE),
                area(2, "near but full", 36.801, 10.181, SpotStatus.RESERVED, SpotStatus.NOT_AVAILABLE),
                area(3, "no location", null, null, SpotStatus.AVAILABLE),
                area(4, "middle", 36.82, 10.20, SpotStatus.OCCUPIED, SpotStatus.AVAILABLE),
            ),
            user,
        )
        assertEquals(listOf("near but full", "middle", "far", "no location"), ranked.map { it.area.name })
        assertEquals("middle", closestVacant(ranked)?.area?.name)
        assertEquals(1, closestVacant(ranked)?.freeSpots)
    }

    @Test
    fun withoutUserLocationThereIsNoClosestArea() {
        val ranked = rankAreas(listOf(area(1, "b", 36.9, 10.3, SpotStatus.AVAILABLE), area(2, "a", 36.8, 10.2)), null)
        assertEquals(listOf("a", "b"), ranked.map { it.area.name })
        assertNull(closestVacant(ranked))
    }

    @Test
    fun parsesTheEditorLayout() {
        // Taken from export.mysql.sql: integers and decimals, angle and missing optional fields.
        val area = ParkingArea(
            1, "hamza",
            """{"width":42,"height":25,"latitude":null,"longitude":null,"floors":[[{"kind":"ROAD","x":0.5,"y":5.5,"width":39.5,"height":5,"angle":0},{"kind":"SPOT","type":"EV","number":"A-01","x":0.5,"y":0.5,"width":2.5,"height":5,"angle":90}]]}""",
        )
        val layout = area.parseLayout()!!
        assertEquals(42.0, layout.width, 0.0)
        assertNull(layout.location)
        assertEquals(ItemKind.SPOT, layout.floors[0][1].kind)
        assertEquals(SpotType.EV, layout.floors[0][1].type)
        assertNull(ParkingArea(2, "broken", "not json").parseLayout())
    }

    @Test
    fun tapHitTestFollowsRotation() {
        // 2.5 x 5 m spot at (10, 10) rotated 90° clockwise around its top-left corner:
        // it now covers x 5..10 and y 10..12.5, like the editor's bounding box.
        val spot = Item(ItemKind.SPOT, SpotType.REGULAR, "A-01", x = 10.0, y = 10.0, width = 2.5, height = 5.0, angle = 90.0)
        assertTrue(spot.contains(7.5, 11.0))
        assertFalse(spot.contains(11.0, 12.0)) // where it was before rotating
        assertTrue(spot.copy(angle = 0.0).contains(11.0, 12.0))
    }

    @Test
    fun billingMatchesTheBackend() {
        val start = Instant.parse("2026-01-01T10:00:00Z")
        assertEquals(1, billedMinutes(start, start))
        assertEquals(1, billedMinutes(start, start.plusSeconds(60)))
        assertEquals(2, billedMinutes(start, start.plusSeconds(61)))
        assertEquals(1.5, cost(1.5, 60), 0.0001)
        assertEquals("45 min", formatDuration(45))
        assertEquals("2 h 05 min", formatDuration(125))
        assertEquals("350 m", formatDistance(350.2))
        assertTrue(formatMoney(0.025, "TND").contains("025")) // TND has 3 decimals
        assertTrue(formatMoney(1.5, "EUR").contains("50"))
    }

    @Test
    fun onlyRegisteredUnreportedReservationsCanBeReportedAsNonPaying() {
        // SCRUM-88: the counter reads the backend's JSON, including fields it does not use.
        val body = """{"id":7,"userId":3,"userName":"Sami","userEmail":"sami@x.tn","spotNumber":"A-01","areaName":"Lac",
            "startedAt":"2026-11-23T08:00:00Z","endedAt":"2026-11-23T09:00:00Z","minutes":60,"amount":1.5,
            "currency":"TND","active":false,"reported":false,"extra":1}"""
        val reservation = json.decodeFromString<CounterReservation>(body)
        assertTrue(reservation.canReport)
        assertFalse(reservation.copy(reported = true).canReport)
        assertFalse(reservation.copy(userId = null, userName = null).canReport) // guest reservation
    }

    @Test
    fun ticketCounterRole() {
        assertTrue(UserAccount(1, "Desk", "c@p.com", role = "TICKET_COUNTER").isTicketCounter)
        assertTrue(UserAccount(1, "Boss", "a@p.com", role = "ADMIN").isTicketCounter)
        assertFalse(UserAccount(1, "Driver", "d@p.com").isTicketCounter)
    }

    @Test
    fun bannedLoginBodyIsReadAsBanInfo() {
        // The backend's 403 for a banned account: the reason and the latest (rejected) appeal.
        val body = """{"banned":true,"message":"Your account is banned","name":"Sami","email":"sami@x.tn",
            "reason":"Left 3 times without paying","bannedAt":"2026-11-20T10:00:00Z",
            "appeal":{"id":4,"message":"I paid later","status":"REJECTED","createdAt":"2026-11-21T09:00:00Z",
            "adminResponse":"No payment found","resolvedAt":"2026-11-22T09:00:00Z"},"canAppeal":true}"""
        val ban = parseBanInfo(body)!!
        assertEquals("Left 3 times without paying", ban.reason)
        assertEquals("REJECTED", ban.appeal!!.status)
        assertEquals("No payment found", ban.appeal!!.adminResponse)
        assertTrue(ban.canAppeal)
        // Any other 403 (e.g. a permission error) is not a ban.
        assertNull(parseBanInfo("""{"message":"Forbidden","status":403}"""))
        assertEquals("Forbidden", errorMessageOf("""{"message":"Forbidden","status":403}"""))
    }
}
