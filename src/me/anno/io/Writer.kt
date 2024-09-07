package me.anno.io

import me.anno.maths.Maths.clamp
import me.anno.utils.InternalAPI
import java.io.OutputStream
import java.nio.ByteBuffer

@InternalAPI
interface Writer {

    var position: Int

    fun writeLE8(v: Int)
    fun writeLE16(v: Int) {
        writeLE8(v)
        writeLE8(v.shr(8))
    }

    fun writeLE32(v: Int) {
        writeLE16(v)
        writeLE16(v.shr(16))
    }

    fun writeLE64(v: Long) {
        writeLE32(v.toInt())
        writeLE32(v.shr(32).toInt())
    }

    fun writeBE8(v: Int) {
        writeLE8(v)
    }

    fun writeBE16(v: Int) {
        writeBE8(v.shr(8))
        writeBE8(v)
    }

    fun writeBE32(v: Int) {
        writeBE16(v.shr(16))
        writeBE16(v)
    }

    fun writeBE64(v: Long) {
        writeBE32(v.shr(32).toInt())
        writeBE32(v.toInt())
    }

    fun writeUTFChar(v: Int) {
        val vi = clamp(v, 0, 0x011000)
        when {
            vi < 0x0080 -> writeLE8(vi)
            vi < 0x0800 -> {
                writeLE8(192 or vi.ushr(6))
                writeLE8(128 or vi.and(63))
            }
            vi <= 0x10000 -> {
                writeLE8(224 or vi.ushr(12).and(15))
                writeLE8(128 or vi.ushr(6).and(63))
                writeLE8(128 or vi.and(63))
            }
            else -> {
                writeLE8(240 or vi.ushr(18).and(7))
                writeLE8(128 or vi.ushr(12).and(63))
                writeLE8(128 or vi.ushr(6).and(63))
                writeLE8(128 or vi.and(63))
            }
        }
    }

    // only supported on some platforms
    fun asByteBuffer(): ByteBuffer

    // only supported on some platforms
    fun asStream(): OutputStream
}