package com.cockpitf30.vehicle.source

import com.cockpitf30.vehicle.Doors
import com.cockpitf30.vehicle.Tyres
import com.cockpitf30.vehicle.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

/** Datos simulados para probar la interfaz fuera del coche. */
class DemoSource : VehicleDataSource {
    override val name = "Demo"

    override fun start(scope: CoroutineScope) {
        scope.launch {
            VehicleRepository.updateStatus { it.copy(connected = true) }
            var t = 0.0
            while (isActive) {
                val speed = (70 + 60 * sin(t / 9)).roundToInt()
                val rpm = (900 + speed * 28 + 600 * sin(t / 2)).roundToInt()
                VehicleRepository.updateState {
                    it.copy(
                        speedKmh = speed,
                        rpm = rpm,
                        outsideTempC = 18.5f,
                        gear = if (speed < 3) "P" else "D",
                        fuelLevel = 0.62f,
                        oilTempC = (60 + t * 2).coerceAtMost(105.0).roundToInt(),
                        rangeKm = 451,
                        tyres = Tyres(2.4f, 2.4f, 2.3f, 2.4f),
                        doors = Doors(false, false, false, false, trunk = (t.toInt() / 40) % 2 == 1, hood = false),
                    )
                }
                VehicleRepository.updateStatus { it.copy(framesReceived = it.framesReceived + 1) }
                t += 0.1
                delay(50)
            }
        }
    }
}
