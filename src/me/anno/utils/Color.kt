package me.anno.utils

import me.anno.config.DefaultStyle.black
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import org.joml.Vector3f
import org.joml.Vector3fc
import org.joml.Vector4f
import org.joml.Vector4fc
import kotlin.math.abs
import kotlin.math.roundToInt

object Color {

    fun Int.r() = shr(16) and 255
    fun Int.g() = shr(8) and 255
    fun Int.b() = this and 255
    fun Int.a() = shr(24) and 255

    fun Int.withAlpha(alpha: Float): Int = rgba(r(), g(), b(), (255 * alpha).roundToInt())
    fun Int.mulAlpha(alpha: Float): Int = rgba(r(), g(), b(), (a() * alpha).roundToInt())
    fun Int.mulARGB(other: Int): Int =
        rgba(r() * other.r() / 255, g() * other.g() / 255, b() * other.b() / 255, a() * other.a() / 255)

    fun rgb(r: Byte, g: Byte, b: Byte): Int = r.toInt().and(255).shl(16) or
            g.toInt().and(255).shl(8) or
            b.toInt().and(255) or
            0xff.shl(24)

    fun rgb(r: Int, g: Int, b: Int): Int = clamp(r, 0, 255).shl(16) or
            clamp(g, 0, 255).shl(8) or
            clamp(b, 0, 255) or
            0xff.shl(24)

    fun rgb(r: Float, g: Float, b: Float): Int =
        rgb((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())

    fun rgba(r: Byte, g: Byte, b: Byte, a: Byte): Int = r.toInt().and(255).shl(16) or
            g.toInt().and(255).shl(8) or
            b.toInt().and(255) or
            a.toInt().and(255).shl(24)

    fun rgba(r: Int, g: Int, b: Int, a: Int): Int = clamp(r, 0, 255).shl(16) or
            clamp(g, 0, 255).shl(8) or
            clamp(b, 0, 255) or
            clamp(a, 0, 255).shl(24)

    fun rgba(r: Float, g: Float, b: Float, a: Float): Int =
        rgba((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt(), (a * 255f).roundToInt())

    fun argb(a: Int, r: Int, g: Int, b: Int) = rgba(r, g, b, a)

    fun Int.hasAlpha() = this.ushr(24) != 255

    fun Vector3fc.hasAlpha() = false

    fun Vector4fc.hasAlpha() = w() < 1f

    fun normARGB(v: Vector3f): Int {
        val r = v.x
        val g = v.y
        val b = v.z
        val div = Maths.max(r, Maths.max(g, b))
        return rgba(r * div, g * div, b * div, 1f)
    }

    fun colorDifference(c0: Int, c1: Int): Int {
        val dr = abs(c0.r() - c1.r())
        val dg = abs(c0.g() - c1.g())
        val db = abs(c0.b() - c1.b())
        return dr + dg + db
    }

    fun Int.toVecRGBA() = Vector4f(r() / 255f, g() / 255f, b() / 255f, a() / 255f)

    const val base36 = "0123456789abcdefghijklmnopqrstuvwxyz"

    fun hex4(i: Long): Char {
        return base36[i.toInt() and 15]
    }

    fun base36(i: Int): Char {
        return base36[i % 36]
    }

    fun hex4(i: Int): Char {
        return base36[i and 15]
    }

    fun hex8(i: Int): String = "${base36[(i shr 4) and 15]}${base36[i and 15]}"
    fun hex16(i: Int) = "${hex4(i shr 12)}${hex4(i shr 8)}${hex4(i shr 4)}${hex4(i)}"
    fun hex24(i: Int) = "${hex8((i shr 16))}${hex16(i)}"
    fun hex32(i: Int) = "${hex16((i shr 16))}${hex16(i)}"
    fun hex8(f: Float) = hex8(clamp((255 * f).roundToInt(), 0, 255))

    fun Int.toHexColor(): String {
        return if (this and black == black) "#${hex24(this)}"
        else "#${hex32(this)}"
    }

    fun Vector3fc.toHexColor(): String {
        return "#${hex8(x())}${hex8(y())}${hex8(z())}"
    }

    fun Vector4fc.toHexColor(): String {
        return "#${if (w() == 1f) "" else hex8(w())}${hex8(x())}${hex8(y())}${hex8(z())}"
    }

    fun Vector4fc.toARGB() = toARGB(255)
    fun Vector4fc.toARGB(scale: Int): Int {
        return clamp((x() * scale).toInt(), 0, 255).shl(16) or
                clamp((y() * scale).toInt(), 0, 255).shl(8) or
                clamp((z() * scale).toInt(), 0, 255) or
                clamp((w() * 255).toInt(), 0, 255).shl(24)
    }

}
