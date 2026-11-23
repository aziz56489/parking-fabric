package com.parking.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.parking.app.ui.theme.ParkingTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.parking.app.ui.AreasScreen
import com.parking.app.ui.DeclareStatusScreen
import com.parking.app.ui.EditAccountScreen
import com.parking.app.ui.LoginScreen
import com.parking.app.ui.PlanScreen
import com.parking.app.ui.ProfileScreen
import com.parking.app.ui.ReceiptScreen
import com.parking.app.ui.ReceiptsScreen
import com.parking.app.ui.RegisterScreen
import com.parking.app.ui.ReservationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParkingTheme {
                ParkingApp()
            }
        }
    }
}

/** The screens and how to go from one to another. One view model is shared by all screens. */
@Composable
fun ParkingApp(vm: ParkingViewModel = viewModel()) {
    val nav = rememberNavController()
    NavHost(
        nav,
        startDestination = "login",
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(tween(280)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(320)) { it / 5 } },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(280)) },
        popExitTransition = { fadeOut(tween(200)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(320)) { it / 5 } },
    ) {
        composable("areas") {
            AreasScreen(
                vm,
                onOpenArea = { nav.navigate("plan/$it") },
                onOpenReservation = { nav.navigate("reservation") },
                onOpenReceipts = { nav.navigate("receipts") },
                onOpenProfile = { nav.navigate(if (vm.isLoggedIn) "profile" else "login") },
            )
        }
        composable("plan/{areaId}") { entry ->
            PlanScreen(
                vm,
                areaId = entry.arguments?.getString("areaId")!!.toInt(),
                onBack = { nav.popBackStack() },
                onReserved = { nav.navigate("reservation") { popUpTo("areas") } },
            )
        }
        composable("reservation") {
            ReservationScreen(
                vm,
                onBack = { nav.popBackStack() },
                onExited = { nav.navigate("receipt/${it.id}") { popUpTo("areas") } },
            )
        }
        composable("receipt/{id}") { entry ->
            ReceiptScreen(vm, reservationId = entry.arguments?.getString("id")!!.toInt(), onBack = { nav.popBackStack() })
        }
        composable("receipts") {
            ReceiptsScreen(vm, onBack = { nav.popBackStack() }, onOpenReceipt = { nav.navigate("receipt/$it") })
        }
        composable("profile") {
            ProfileScreen(
                vm,
                onBack = { nav.popBackStack() },
                onNavigateLogin = { nav.navigate("login") },
                onNavigateEdit = { nav.navigate("edit_account") },
                onNavigateDeclareStatus = { nav.navigate("declare_status") },
            )
        }
        composable("login") {
            LoginScreen(
                vm,
                // Login is the start screen: only offer "back" when there is somewhere to go back to.
                onBack = if (nav.previousBackStackEntry != null) ({ nav.popBackStack() }) else null,
                onLoggedIn = {
                    nav.navigate("areas") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateRegister = { nav.navigate("register") },
            )
        }
        composable("register") {
            RegisterScreen(
                vm,
                onBack = { nav.popBackStack() },
                onRegistered = {
                    nav.navigate("areas") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateLogin = { nav.navigate("login") { popUpTo("register") { inclusive = true } } },
            )
        }
        composable("edit_account") {
            EditAccountScreen(
                vm,
                onBack = { nav.popBackStack() },
            )
        }
        composable("declare_status") {
            DeclareStatusScreen(
                vm,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
