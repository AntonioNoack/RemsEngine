package me.anno.utils

import me.anno.config.DefaultStyle.black
import me.anno.utils.Maths.clamp
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.roundToInt

object Color {

    fun Int.r() = shr(16) and 255
    fun Int.g() = shr(8) and 255
    fun Int.b() = this and 255
    fun Int.a() = shr(24) and 255

    fun rgba(r: Int, g: Int, b: Int, a: Int): Int = clamp(r, 0, 255).shl(16) or
            clamp(g, 0, 255).shl(8) or
            clamp(b, 0, 255) or
            clamp(a, 0, 255).shl(24)

    fun rgba(r: Float, g: Float, b: Float, a: Float): Int =
        rgba((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt(), (a * 255).roundToInt())

    fun colorDifference(c0: Int, c1: Int): Int {
        val dr = abs(c0.r() - c1.r())
        val dg = abs(c0.g() - c1.g())
        val db = abs(c0.b() - c1.b())
        return dr + dg + db
    }

    fun Int.toVecRGBA() = Vector4f(r() / 255f, g() / 255f, b() / 255f, a() / 255f)

    fun hex8(i: Int): String {
        val hex = "0123456789abcdef"
        return "${hex[(i shr 4) and 15]}${hex[i and 15]}"
    }

    fun hex16(i: Int) = "${hex8((i shr 8))}${hex8(i)}"
    fun hex24(i: Int) = "${hex8((i shr 16))}${hex16(i)}"
    fun hex32(i: Int) = "${hex16((i shr 16))}${hex16(i)}"
    fun hex8(f: Float) = hex8(clamp((255 * f).roundToInt(), 0, 255))

    fun Int.toHexColor(): String {
        return if(this and black == black) "#${hex24(this)}"
        else "#${hex32(this)}"
    }

    fun Vector3f.toHexColor(): String {
        return "#${hex8(x)}${hex8(y)}${hex8(z)}"
    }

    fun Vector4f.toHexColor(): String {
        return "#${if (w == 1f) "" else hex8(w)}${hex8(x)}${hex8(y)}${hex8(z)}"
    }

    fun Vector4f.toARGB() = toARGB(255)
    fun Vector4f.toARGB(scale: Int): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                clamp((w * 255).toInt(), 0, 255).shl(24)
    }

}
