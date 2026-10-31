package com.cockpitf30.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cockpitf30.input.IDriveInput
import com.cockpitf30.input.NavAction
import com.cockpitf30.service.VehicleService
import com.cockpitf30.system.AppSettings
import com.cockpitf30.system.BatteryOptimization
import com.cockpitf30.ui.components.DefaultFocus
import com.cockpitf30.ui.components.SideItem
import com.cockpitf30.ui.components.Sidebar
import com.cockpitf30.ui.screens.AppsScreen
import com.cockpitf30.ui.screens.DiagnosticsScreen
import com.cockpitf30.ui.screens.HomeScreen
import com.cockpitf30.ui.screens.Screen
import com.cockpitf30.ui.screens.VehicleScreen
import com.cockpitf30.ui.screens.Reorder
import com.cockpitf30.ui.screens.WeatherScreen
import com.cockpitf30.ui.screens.PhoneScreen
import com.cockpitf30.weather.WeatherRepository
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.cockpitf30.ui.screens.openMaps
import com.cockpitf30.ui.screens.openMusic
import com.cockpitf30.ui.theme.Cockpit
import com.cockpitf30.ui.theme.CockpitTheme
import com.cockpitf30.vehicle.VehicleRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MainActivity : ComponentActivity() {
    private val navActions = MutableSharedFlow<NavAction>(extraBufferCapacity = 16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars()
        VehicleService.start(this)
        askBatteryExemptionOnce()

        setContent {
            CockpitTheme {
                CockpitRoot(navActions)
            }
        }
    }

    // Tras tocar la pantalla el foco deja de verse; el próximo giro de la ruleta lo volverá a colocar.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) DefaultFocus.needsPlacement = true
        return super.dispatchTouchEvent(ev)
    }

    // Interceptamos aquí, antes que las vistas, para que la ruleta siempre mueva el foco.
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        VehicleRepository.logInput(IDriveInput.describe(event))
        val action = IDriveInput.fromMotion(event) ?: return super.dispatchGenericMotionEvent(event)
        navActions.tryEmit(action)
        return true
    }

    // Falso positivo de lint: sobrescribir dispatchKeyEvent es API pública de Activity.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        VehicleRepository.logInput(IDriveInput.describe(event))
        // Tras una pulsación larga de OK, nos comemos sus repeticiones y el soltar para que no haga clic.
        if (swallowCenter && IDriveInput.isCenter(event.keyCode)) {
            if (event.action == KeyEvent.ACTION_UP) swallowCenter = false
            return true
        }
        val action = IDriveInput.fromKey(event) ?: return super.dispatchKeyEvent(event)
        if (action == NavAction.LONG_PRESS) swallowCenter = true
        navActions.tryEmit(action)
        return true
    }

    private var swallowCenter = false

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun askBatteryExemptionOnce() {
        val settings = AppSettings(this)
        if (!settings.askedBatteryExemption && !BatteryOptimization.isIgnoring(this)) {
            settings.askedBatteryExemption = true
            BatteryOptimization.request(this)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class) // requestInputMode
@Composable
private fun CockpitRoot(navActions: SharedFlow<NavAction>) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val inputMode = LocalInputModeManager.current
    LaunchedEffect(navActions) {
        navActions.collect {
            if (it == NavAction.LONG_PRESS) {
                Reorder.longPress.tryEmit(Unit)
                return@collect
            }
            // Tras tocar la pantalla Android entra en modo táctil y nada es enfocable:
            // al girar la ruleta volvemos a modo teclado para que el foco se mueva y se vea.
            if (inputMode.inputMode != InputMode.Keyboard) inputMode.requestInputMode(InputMode.Keyboard)
            if (DefaultFocus.needsPlacement) {
                DefaultFocus.needsPlacement = false
                // Nada tenía el foco: el primer giro solo lo coloca en el elemento por defecto de la pantalla.
                if (Reorder.moving.value == null && DefaultFocus.request()) return@collect
            }
            val dir = if (it == NavAction.NEXT) 1 else -1
            // En modo mover, la ruleta desplaza la tarjeta en vez del foco.
            val step = Reorder.step
            if (Reorder.moving.value != null && step != null) step(dir)
            else focusManager.moveFocus(if (dir > 0) FocusDirection.Next else FocusDirection.Previous)
        }
    }

    // Tiempo: refresco cada 30 min y, la primera vez, pedir permiso de ubicación.
    val ctx = LocalContext.current
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        scope.launch { WeatherRepository.refresh(ctx) }
    }
    LaunchedEffect(Unit) {
        val settings = AppSettings(ctx)
        WeatherRepository.loadCache(ctx)
        if (!settings.askedLocation && settings.weatherPlace == null && !WeatherRepository.hasLocationPermission(ctx)) {
            settings.askedLocation = true
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }
        while (true) {
            WeatherRepository.refresh(ctx)
            delay(30 * 60 * 1000L)
        }
    }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }

    val context = LocalContext.current
    val sideItems = listOf(
        SideItem("Home", Icons.Filled.Home, screen == Screen.HOME) { screen = Screen.HOME },
        SideItem("Navegación", Icons.Filled.LocationOn, false) { openMaps(context) },
        SideItem("Música", Icons.Filled.PlayArrow, false) { openMusic(context) },
        SideItem("Apps", Icons.Filled.Menu, screen == Screen.APPS) { screen = Screen.APPS },
        // Ajustes de la app: fuente de datos, puerto serie, batería.
        SideItem("Ajustes", Icons.Filled.Settings, screen == Screen.DIAGNOSTICS) { screen = Screen.DIAGNOSTICS },
    )

    Row(
        Modifier
            .fillMaxSize()
            .background(Cockpit.Background)
            .background(Brush.radialGradient(listOf(Cockpit.Blue.copy(alpha = 0.10f), Cockpit.Background), radius = 1600f)),
    ) {
        Sidebar(sideItems)
        AnimatedContent(
            screen,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.HOME -> HomeScreen(onOpen = { screen = it })
                Screen.VEHICLE -> VehicleScreen()
                Screen.APPS -> AppsScreen()
                Screen.DIAGNOSTICS -> DiagnosticsScreen()
                Screen.WEATHER -> WeatherScreen()
                Screen.PHONE -> PhoneScreen()
            }
        }
    }
}
