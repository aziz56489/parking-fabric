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

/** All the app state and database-backed authentication with automatic backend discovery. */
class ParkingViewModel(application: Application) : AndroidViewModel(application) {

    private var api = ParkingApi.create(application)
    private val locationClient = LocationServices.getFusedLocationProviderClient(application)

    // Older app builds stored a UUID String here. Android throws ClassCastException
    // when getInt reads that value, so accept either format during the migration.
    private var userId: Int = readStoredUserId(
        application.getSharedPreferences("parking", Context.MODE_PRIVATE),
    )

    var currentUser by mutableStateOf<UserAccount?>(null)
        private set
    val isLoggedIn: Boolean
        get() = currentUser != null

    init {
        val prefs = application.getSharedPreferences("parking", Context.MODE_PRIVATE)
        val savedUserId = readStoredUserId(prefs)
        val savedName = prefs.getString("userName", null)
        val savedEmail = prefs.getString("userEmail", null)
        val savedRole = prefs.getString("userRole", "USER")
        val savedHandicapped = prefs.getBoolean("isHandicapped", false)
        val savedEv = prefs.getBoolean("isEvOwner", false)
        val savedBlocked = prefs.getBoolean("isBlocked", false)

        if (savedUserId != 0 && savedEmail != null) {
            currentUser = UserAccount(savedUserId, savedName ?: "User", savedEmail, savedRole ?: "USER", savedHandicapped, savedEv, savedBlocked)
            userId = savedUserId
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

    /** Loads areas from the configured backend URL. */
    fun refreshAreas() = launch {
        try {
            allAreas = api.areas()
        } catch (e: Exception) {
            message = "Cannot load parking areas: ${e.userMessage()}"
        } finally {
            areasLoaded = true
        }
    }

    fun refreshReservations() = launch {
        if (userId != 0) {
            try {
                history = api.reservations(userId)
                reservation = history.firstOrNull { it.active }
            } catch (e: Exception) {
                // Ignore if offline
            }
        }
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
        try {
            currentArea = AreaInfo.of(api.area(id), location)
        } catch (e: Exception) {
            message = "Cannot load area from server: ${e.userMessage()}"
        }
    }

    fun reserve(spot: ParkingSpot, onReserved: () -> Unit) = launch(showWorking = true) {
        if (userId == 0) {
            message = "Please log in to make a reservation"
            return@launch
        }
        try {
            reservation = api.reserve(ReservationRequest(spot.id, userId))
            onReserved()
        } catch (e: Exception) {
            message = "Reservation failed: ${e.userMessage()}"
        } finally {
            currentArea?.let { openArea(it.area.id) }
        }
    }

    fun exit(onExited: (Reservation) -> Unit) = launch(showWorking = true) {
        val active = reservation ?: return@launch
        try {
            val ended = api.exit(active.id)
            reservation = null
            receipt = ended
            history = listOf(ended) + history.filter { it.id != ended.id }
            onExited(ended)
        } catch (e: Exception) {
            message = "Exit failed: ${e.userMessage()}"
        }
    }

    fun reportIssue(type: IssueType, description: String) = launch(showWorking = true) {
        val active = reservation ?: return@launch
        try {
            api.reportIssue(active.id, IssueRequest(type, description))
            message = if (type == IssueType.SPOT_TAKEN) {
                "Report sent. You can mark your exit and choose another spot."
            } else {
                "Report sent, thank you"
            }
        } catch (e: Exception) {
            message = "Report failed: ${e.userMessage()}"
        }
    }

    // SCRUM-88: As a ticket counter, I can report a non paying user.
    var counterReservations by mutableStateOf<List<CounterReservation>>(emptyList())
        private set
    var nonPayingReports by mutableStateOf<List<NonPayingReport>>(emptyList())
        private set
    var counterLoaded by mutableStateOf(false)
        private set

    fun refreshCounter() = launch {
        try {
            counterReservations = api.counterReservations()
            nonPayingReports = api.nonPayingReports()
        } catch (e: Exception) {
            message = if (e.isSessionExpired()) expireSession() else "Cannot load the counter: ${e.userMessage()}"
        } finally {
            counterLoaded = true
        }
    }

    fun reportNonPaying(reservation: CounterReservation, note: String, onSuccess: () -> Unit) = launch(showWorking = true) {
        try {
            val csrfToken = api.csrf().token
            val report = api.reportNonPaying(csrfToken, NonPayingReportRequest(reservation.id, note.trim()))
            nonPayingReports = listOf(report) + nonPayingReports
            counterReservations = counterReservations.map { if (it.id == reservation.id) it.copy(reported = true) else it }
            message = "${report.userName ?: "The user"} was reported as non paying"
            onSuccess()
        } catch (e: Exception) {
            message = if (e.isSessionExpired()) expireSession() else "Report failed: ${e.userMessage()}"
        }
    }

    fun loadReceipt(id: Int) = launch {
        try {
            receipt = history.firstOrNull { it.id == id } ?: api.reservation(id)
        } catch (e: Exception) {
            receipt = history.firstOrNull { it.id == id }
        }
    }

    fun register(name: String, email: String, pass: String, onSuccess: () -> Unit) = launch(showWorking = true) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            message = "Please fill in all fields"
            return@launch
        }
        try {
            val account = api.register(RegisterRequest(name, email, pass))
            currentUser = account
            userId = account.id
            saveUser(account)
            refreshReservations()
            onSuccess()
        } catch (e: Exception) {
            message = "Registration failed: ${e.userMessage()}"
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit, onBanned: () -> Unit) = launch(showWorking = true) {
        if (email.isBlank() || pass.isBlank()) {
            message = "Please fill in all fields"
            return@launch
        }
        try {
            val account = api.login(LoginRequest(email, pass))
            currentUser = account
            userId = account.id
            saveUser(account)
            refreshReservations()
            onSuccess()
        } catch (e: Exception) {
            val body = (e as? retrofit2.HttpException)?.takeIf { it.code() == 403 }
                ?.let { runCatching { it.response()?.errorBody()?.string() }.getOrNull() }
            val ban = body?.let(::parseBanInfo)
            if (ban != null) {
                banInfo = ban
                banCredentials = email.trim() to pass
                onBanned()
            } else {
                message = "Login failed: ${body?.let(::errorMessageOf) ?: e.userMessage()}"
            }
        }
    }

    // A banned user sees why and can appeal. They can't log in, so the email and password typed
    // at login are kept in memory (never saved) while the "account suspended" screen is open.
    var banInfo by mutableStateOf<BanInfo?>(null)
        private set
    private var banCredentials: Pair<String, String>? = null

    fun refreshBanStatus() = launch {
        val (email, password) = banCredentials ?: return@launch
        try {
            banInfo = api.banStatus(BanStatusRequest(email, password))
        } catch (e: Exception) {
            message = "Cannot refresh: ${e.userMessage()}"
        }
    }

    fun sendAppeal(text: String, onSent: () -> Unit) = launch(showWorking = true) {
        val (email, password) = banCredentials ?: return@launch
        if (text.isBlank()) {
            message = "Please explain why the ban should be lifted"
            return@launch
        }
        try {
            banInfo = api.appeal(AppealRequest(email, password, text.trim()))
            message = "Appeal sent. An admin will review it."
            onSent()
        } catch (e: Exception) {
            message = "Appeal failed: ${e.userMessage()}"
        }
    }

    /** Leaving the suspended screen forgets the password. */
    fun leaveBanScreen() {
        banInfo = null
        banCredentials = null
    }

    /** A user banned while logged in: sign out, and send them to login, where they will see why. */
    fun checkAccount(onBanned: () -> Unit) = launch {
        if (currentUser == null) return@launch
        try {
            val account = api.me()
            if (account.isBlocked) {
                clearLocalSession()
                message = "Your account has been banned. Log in to see why and to appeal."
                onBanned()
            } else {
                currentUser = account
                saveUser(account)
            }
        } catch (e: Exception) {
            // Offline or session gone: keep the local account; actions will report the problem.
        }
    }

    fun logout(onSuccess: () -> Unit) {
        clearLocalSession()
        onSuccess()
    }

    /** The server no longer knows this session (app reinstalled, backend restarted...): sign out locally. */
    private fun expireSession(): String {
        clearLocalSession()
        return "Your session has expired. Please log in again."
    }

    private fun Exception.isSessionExpired() = this is retrofit2.HttpException && code() == 401

    private fun clearLocalSession() {
        currentUser = null
        userId = 0
        val prefs = getApplication<Application>().getSharedPreferences("parking", Context.MODE_PRIVATE)
        ParkingApi.clearSession(getApplication())
        prefs.edit().remove("userId").remove("userEmail").remove("userName").remove("userRole").remove("isHandicapped").remove("isEvOwner").remove("isBlocked").apply()
        history = emptyList()
        reservation = null
        counterReservations = emptyList()
        nonPayingReports = emptyList()
        counterLoaded = false
    }

    fun updateAccount(name: String, email: String, onSuccess: () -> Unit) = launch(showWorking = true) {
        val current = currentUser
        if (current == null) {
            message = "Not logged in"
            return@launch
        }
        try {
            val csrfToken = api.csrf().token
            val updated = api.updateUser(current.id, csrfToken, UpdateUserRequest(name, email, current.isHandicapped, current.isEvOwner))
            currentUser = updated
            saveUser(updated)
            message = "Account updated successfully"
            onSuccess()
        } catch (e: Exception) {
            message = if (e.isSessionExpired()) expireSession() else "Account update failed: ${e.userMessage()}"
        }
    }

    fun declareStatus(isHandicapped: Boolean, isEvOwner: Boolean, onSuccess: () -> Unit) = launch(showWorking = true) {
        val current = currentUser
        if (current == null) {
            message = "Not logged in"
            return@launch
        }
        try {
            val csrfToken = api.csrf().token
            val updated = api.updateUser(current.id, csrfToken, UpdateUserRequest(current.name, current.email, isHandicapped, isEvOwner))
            currentUser = updated
            saveUser(updated)
            message = "Status updated successfully"
            onSuccess()
        } catch (e: Exception) {
            message = if (e.isSessionExpired()) expireSession() else "Status update failed: ${e.userMessage()}"
        }
    }

    private fun saveUser(account: UserAccount) {
        val prefs = getApplication<Application>().getSharedPreferences("parking", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("userId", account.id)
            .putString("userName", account.name)
            .putString("userEmail", account.email)
            .putString("userRole", account.role)
            .putBoolean("isHandicapped", account.isHandicapped)
            .putBoolean("isEvOwner", account.isEvOwner)
            .putBoolean("isBlocked", account.isBlocked)
            .apply()
    }

    private fun readStoredUserId(prefs: android.content.SharedPreferences): Int =
        runCatching { prefs.getInt("userId", 0) }.getOrElse {
            prefs.getString("userId", null)?.toIntOrNull() ?: 0
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
