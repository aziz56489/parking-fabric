package com.parking.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/** All the app state. Screens read these properties and call these functions. */
class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private val api = ParkingApi.create()
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    /** There are no accounts yet: a random id, kept on the phone, identifies the user's reservations. */
    private var userId: String = application.getSharedPreferences("parking", Context.MODE_PRIVATE).let { prefs ->
        prefs.getString("userId", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("userId", it).apply()
        }
    }

    private var allAreas by mutableStateOf<List<ParkingArea>>(emptyList())
    var areasLoaded by mutableStateOf(false)
        private set
    var location by mutableStateOf<GeoPoint?>(null)
        private set
    var locating by mutableStateOf(false)
        private set

    /** Areas sorted by distance from the user, recomputed when the areas or the location change. */
    val areas by derivedStateOf { rankAreas(allAreas, location) }
    val closestVacant by derivedStateOf { closestVacant(areas) }

    /** The area shown on the plan screen. */
    var currentArea by mutableStateOf<AreaInfo?>(null)
        private set
    var reservation by mutableStateOf<Reservation?>(null)
        private set
    var history by mutableStateOf<List<Reservation>>(emptyList())
        private set
    var receipt by mutableStateOf<Reservation?>(null)
        private set
    var working by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    fun refreshAreas() = launch {
        try {
            allAreas = api.areas()
        } finally {
            areasLoaded = true
        }
    }

    fun refreshReservations() = launch {
        history = api.reservations(userId)
        reservation = history.firstOrNull { it.active }
    }

    @SuppressLint("MissingPermission") // The screen asks for the permission before calling this.
    fun locate() = launch {
        locating = true
        try {
            val found = locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                ?: locationClient.lastLocation.await()
            if (found != null) {
                location = GeoPoint(found.latitude, found.longitude)
            } else {
                message = "Could not get your location. Is location turned on?"
            }
        } finally {
            locating = false
        }
    }

    fun openArea(id: Int) = launch {
        if (currentArea?.area?.id != id) currentArea = null
        currentArea = AreaInfo.of(api.area(id), location)
    }

    fun reserve(spot: ParkingSpot, onReserved: () -> Unit) = launch(showWorking = true) {
        try {
            reservation = api.reserve(ReservationRequest(spot.id, userId))
            onReserved()
        } finally {
            currentArea?.let { openArea(it.area.id) } // Spot statuses changed, or someone was faster.
        }
    }

    fun exit(onExited: (Reservation) -> Unit) = launch(showWorking = true) {
        val active = reservation ?: return@launch
        val ended = api.exit(active.id)
        reservation = null
        receipt = ended
        history = listOf(ended) + history.filter { it.id != ended.id }
        onExited(ended)
    }

    fun reportIssue(type: IssueType, description: String) = launch(showWorking = true) {
        val active = reservation ?: return@launch
        api.reportIssue(active.id, IssueRequest(type, description))
        message = if (type == IssueType.SPOT_TAKEN) {
            "Report sent. You can mark your exit and choose another spot."
        } else {
            "Report sent, thank you"
        }
    }

    fun loadReceipt(id: Int) = launch {
        receipt = history.firstOrNull { it.id == id } ?: api.reservation(id)
    }

    /** Runs [block] and turns any error into a message shown by the screen. */
    private fun launch(showWorking: Boolean = false, block: suspend () -> Unit) = viewModelScope.launch {
        if (showWorking) working = true
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            message = e.userMessage()
        } finally {
            if (showWorking) working = false
        }
    }
}
