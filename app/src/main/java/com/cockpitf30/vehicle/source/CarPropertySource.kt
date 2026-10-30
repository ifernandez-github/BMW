package com.cockpitf30.vehicle.source

import android.content.Context
import android.content.pm.PackageManager
import com.cockpitf30.vehicle.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Lee velocidad y RPM de CarPropertyManager (Android Automotive).
 *
 * La mayoría de radios chinas usan Android "normal", no Automotive, así que lo esperable es
 * que [isAvailable] devuelva false. Se accede por reflexión para no depender del SDK de coche.
 */
class CarPropertySource(private val context: Context) : VehicleDataSource {
    override val name = "CarPropertyManager"

    private var car: Any? = null

    override fun start(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val manager = try {
                connect()
            } catch (e: Exception) {
                VehicleRepository.updateStatus { it.copy(connected = false, lastError = "android.car: ${e.message ?: e.javaClass.simpleName}") }
                return@launch
            }
            VehicleRepository.updateStatus { it.copy(connected = true, lastError = null) }
            val getFloat = manager.javaClass.getMethod("getFloatProperty", Int::class.java, Int::class.java)
            while (isActive) {
                val speedMs = runCatching { getFloat.invoke(manager, PERF_VEHICLE_SPEED, 0) as Float }.getOrNull()
                val rpm = runCatching { getFloat.invoke(manager, ENGINE_RPM, 0) as Float }.getOrNull()
                VehicleRepository.updateState { s ->
                    s.copy(
                        speedKmh = speedMs?.let { (it * 3.6f).roundToInt() } ?: s.speedKmh,
                        rpm = rpm?.roundToInt() ?: s.rpm,
                    )
                }
                VehicleRepository.updateStatus { it.copy(framesReceived = it.framesReceived + 1) }
                delay(POLL_MS)
            }
        }
    }

    override fun stop() {
        runCatching { car?.javaClass?.getMethod("disconnect")?.invoke(car) }
        car = null
    }

    private fun connect(): Any {
        val carClass = Class.forName("android.car.Car")
        val instance = carClass.getMethod("createCar", Context::class.java).invoke(null, context)
            ?: error("createCar devolvió null")
        car = instance
        return carClass.getMethod("getCarManager", String::class.java).invoke(instance, "property")
            ?: error("sin servicio de propiedades")
    }

    companion object {
        private const val POLL_MS = 200L
        // Valores de android.car.VehiclePropertyIds
        private const val PERF_VEHICLE_SPEED = 0x11600207
        private const val ENGINE_RPM = 0x11600305

        fun isAvailable(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) &&
                runCatching { Class.forName("android.car.Car") }.isSuccess
    }
}
