package com.cockpitf30.vehicle.raise

/**
 * Trama Raise (RZC) ya validada.
 *
 * Formato asumido (el usado por Raise en otros modelos; SIN VERIFICAR en el F30):
 *   0x2E | tipo | longitud | datos[longitud] | checksum
 *   checksum = (tipo + longitud + Σdatos) & 0xFF  XOR 0xFF
 */
class RaiseFrame(val type: Int, val data: ByteArray) {
    fun u8(index: Int): Int? = data.getOrNull(index)?.toInt()?.and(0xFF)

    fun toHex(): String = buildString {
        append("2E %02X %02X |".format(type, data.size))
        data.forEach { append(" %02X".format(it.toInt() and 0xFF)) }
    }
}

/**
 * Convierte un flujo de bytes del puerto serie en tramas. Tolera basura entre tramas
 * y tramas partidas entre lecturas.
 */
class RaiseFramer(
    private val onFrame: (RaiseFrame) -> Unit,
    private val onBadChecksum: (ByteArray) -> Unit = {},
) {
    private enum class Step { HEAD, TYPE, LENGTH, DATA, CHECKSUM }

    private var step = Step.HEAD
    private var type = 0
    private var length = 0
    private var data = ByteArray(0)
    private var filled = 0

    fun feed(bytes: ByteArray, count: Int = bytes.size) {
        for (i in 0 until count) feedByte(bytes[i].toInt() and 0xFF)
    }

    private fun feedByte(b: Int) {
        when (step) {
            Step.HEAD -> if (b == HEADER) step = Step.TYPE
            Step.TYPE -> {
                type = b
                step = Step.LENGTH
            }
            Step.LENGTH -> {
                length = b
                data = ByteArray(length)
                filled = 0
                step = if (length == 0) Step.CHECKSUM else Step.DATA
            }
            Step.DATA -> {
                data[filled++] = b.toByte()
                if (filled == length) step = Step.CHECKSUM
            }
            Step.CHECKSUM -> {
                step = Step.HEAD
                if (b == checksum(type, data)) {
                    onFrame(RaiseFrame(type, data))
                } else {
                    onBadChecksum(byteArrayOf(HEADER.toByte(), type.toByte(), length.toByte()) + data + b.toByte())
                }
            }
        }
    }

    companion object {
        const val HEADER = 0x2E

        fun checksum(type: Int, data: ByteArray): Int {
            var sum = type + data.size
            data.forEach { sum += it.toInt() and 0xFF }
            return (sum and 0xFF) xor 0xFF
        }
    }
}
