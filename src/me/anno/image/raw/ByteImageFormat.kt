package me.anno.image.raw

import me.anno.utils.Color.a
import me.anno.utils.Color.argb
import me.anno.utils.Color.b
import me.anno.utils.Color.black
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgb
import me.anno.utils.Color.rgba

enum class ByteImageFormat(val numChannels: Int) {
    // red only
    R(1),

    // red-green
    RG(2),

    // rgb
    RGB(3),
    BGR(3),

    // rgba
    RGBA(4),
    ARGB(4),
    BGRA(4);

    fun fromBytes(src: ByteArray, i: Int, hasAlphaChannel: Boolean): Int {
        return when (this) {
            // alpha-channel is just here for consistency; technically not needed, because Image.hasAlphaChannel = false
            R -> (src[i].toInt().and(255) * 0x10101) or black
            RG -> rgb(src[i], src[i + 1], 0)
            RGB -> rgb(src[i], src[i + 1], src[i + 2])
            BGR -> rgb(src[i + 2], src[i + 1], src[i])
            RGBA -> {
                val a = if (hasAlphaChannel) src[i + 3] else -1
                rgba(src[i], src[i + 1], src[i + 2], a)
            }
            ARGB -> {
                val a = if (hasAlphaChannel) src[i + 3] else -1
                argb(src[i], src[i + 1], src[i + 2], a)
            }
            BGRA -> {
                val b = src[i]
                val g = src[i + 1]
                val r = src[i + 2]
                val a = if (hasAlphaChannel) src[i + 3] else -1
                argb(a, r, g, b)
            }
        }
    }

    fun toBytes(color: Int, dst: ByteArray, i: Int) {
        val r = color.r().toByte()
        val g = color.g().toByte()
        val b = color.b().toByte()
        val a = color.a().toByte()
        when (this) {
            R -> dst[i] = r
            RG -> {
                dst[i] = r
                dst[i + 1] = g
            }
            RGB -> {
                dst[i] = r
                dst[i + 1] = g
                dst[i + 2] = b
            }
            BGR -> {
                dst[i] = b
                dst[i + 1] = g
                dst[i + 2] = r
            }
            RGBA -> {
                dst[i] = r
                dst[i + 1] = g
                dst[i + 2] = b
                dst[i + 3] = a
            }
            ARGB -> {
                dst[i] = a
                dst[i + 1] = r
                dst[i + 2] = g
                dst[i + 3] = b
            }
            BGRA -> {
                dst[i] = b
                dst[i + 1] = g
                dst[i + 2] = r
                dst[i + 3] = a
            }
        }
    }

    companion object {
        fun getRGBAFormat(numChannels: Int): ByteImageFormat {
            return when (numChannels) {
                1 -> R
                2 -> RG
                3 -> RGB
                else -> RGBA
            }
        }
    }
}