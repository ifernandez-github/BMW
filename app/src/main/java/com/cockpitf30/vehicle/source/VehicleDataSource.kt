package com.cockpitf30.vehicle.source

import kotlinx.coroutines.CoroutineScope

/** Una fuente de datos del coche. Escribe en [com.cockpitf30.vehicle.VehicleRepository]. */
interface VehicleDataSource {
    val name: String

    /** Arranca la lectura dentro de [scope]. Debe terminar cuando el scope se cancele. */
    fun start(scope: CoroutineScope)

    fun stop() {}
}
