package me.anno.utils

import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import org.joml.*
import kotlin.math.abs
import kotlin.math.roundToInt

object Color {

    const val black = 0xff000000.toInt()
    const val white = -1

    @JvmField
    val black4 = Vector4f(0f)
    @JvmField
    val black3 = Vector3f(0f)
    @JvmField
    val white4 = Vector4f(1f)
    @JvmField
    val white3 = Vector3f(1f)

    @JvmStatic
    fun Int.r() = shr(16) and 255
    @JvmStatic
    fun Int.g() = shr(8) and 255
    @JvmStatic
    fun Int.b() = this and 255
    @JvmStatic
    fun Int.a() = ushr(24)

    @JvmStatic
    fun Int.r01() = (shr(16) and 255) / 255f
    @JvmStatic
    fun Int.g01() = (shr(8) and 255) / 255f
    @JvmStatic
    fun Int.b01() = (this and 255) / 255f
    @JvmStatic
    fun Int.a01() = ushr(24) / 255f

    @JvmStatic
    fun Int.withAlpha(alpha: Float): Int = rgba(r(), g(), b(), (255f * alpha).roundToInt())
    @JvmStatic
    fun Int.mulAlpha(alpha: Float): Int = rgba(r(), g(), b(), (a() * alpha).roundToInt())

    @JvmStatic
    fun Int.withAlpha(alpha: Int): Int = rgba(r(), g(), b(), alpha)
    @JvmStatic
    fun Int.mulARGB(other: Int): Int =
        rgba((r() * other.r()) / 255, (g() * other.g()) / 255, (b() * other.b()) / 255, (a() * other.a()) / 255)

    @JvmStatic
    fun rgb(r: Byte, g: Byte, b: Byte): Int =
        r.toInt().and(255).shl(16) or
                g.toInt().and(255).shl(8) or
                b.toInt().and(255) or
                0xff.shl(24)

    @JvmStatic
    fun rgb(r: Int, g: Int, b: Int): Int =
        clamp(r, 0, 255).shl(16) or
            clamp(g, 0, 255).shl(8) or
            clamp(b, 0, 255) or
            0xff.shl(24)

    @JvmStatic
    fun rgb(r: Float, g: Float, b: Float): Int =
        rgb((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())

    @JvmStatic
    fun rgba(r: Byte, g: Byte, b: Byte, a: Byte): Int = r.toInt().and(255).shl(16) or
            g.toInt().and(255).shl(8) or
            b.toInt().and(255) or
            a.toInt().and(255).shl(24)

    @JvmStatic
    fun rgba(r: Int, g: Int, b: Int, a: Int): Int = clamp(r, 0, 255).shl(16) or
            clamp(g, 0, 255).shl(8) or
            clamp(b, 0, 255) or
            clamp(a, 0, 255).shl(24)

    @JvmStatic
    fun rgba(r: Float, g: Float, b: Float, a: Float): Int =
        rgba((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt(), (a * 255f).roundToInt())

    @JvmStatic
    fun argb(a: Int, r: Int, g: Int, b: Int) = rgba(r, g, b, a)

    @JvmStatic
    fun Int.hasAlpha() = this.ushr(24) != 255

    @JvmStatic
    fun Vector3f.hasAlpha() = false

    @JvmStatic
    fun Vector4f.hasAlpha() = w < 1f

    @JvmStatic
    fun normARGB(v: Vector3f): Int {
        val r = v.x
        val g = v.y
        val b = v.z
        val div = Maths.max(r, Maths.max(g, b))
        return rgba(r * div, g * div, b * div, 1f)
    }

    @JvmStatic
    fun colorDifference(c0: Int, c1: Int): Int {
        val dr = abs(c0.r() - c1.r())
        val dg = abs(c0.g() - c1.g())
        val db = abs(c0.b() - c1.b())
        return dr + dg + db
    }

    @JvmStatic
    fun Int.toVecRGBA(dst: Vector4f = Vector4f()) = dst.set(r01(), g01(), b01(), a01())
    @JvmStatic
    fun Int.toVecRGB(dst: Vector3f = Vector3f()) = dst.set(r01(), g01(), b01())

    const val base36 = "0123456789abcdefghijklmnopqrstuvwxyz"

    @JvmStatic
    fun hex4(i: Long): Char {
        return base36[i.toInt() and 15]
    }

    @JvmStatic
    fun base36(i: Int): Char {
        return base36[i % 36]
    }

    @JvmStatic
    fun hex4(i: Int): Char {
        return base36[i and 15]
    }

    @JvmStatic
    fun hex8(i: Int) = "${base36[(i shr 4) and 15]}${base36[i and 15]}"
    @JvmStatic
    fun hex16(i: Int) = "${hex4(i shr 12)}${hex4(i shr 8)}${hex4(i shr 4)}${hex4(i)}"
    @JvmStatic
    fun hex24(i: Int) = "${hex8((i shr 16))}${hex16(i)}"
    @JvmStatic
    fun hex32(i: Int) = "${hex16((i shr 16))}${hex16(i)}"
    @JvmStatic
    fun hex8(f: Float) = hex8(clamp((255 * f).roundToInt(), 0, 255))

    @JvmStatic
    fun Int.toHexColor(): String {
        return if (this and black == black) "#${hex24(this)}"
        else "#${hex32(this)}"
    }

    @JvmStatic
    fun Vector3f.toHexColor(): String {
        return "#${hex8(x)}${hex8(y)}${hex8(z)}"
    }

    @JvmStatic
    fun Vector4f.toHexColor(): String {
        return "#${if (w == 1f) "" else hex8(w)}${hex8(x)}${hex8(y)}${hex8(z)}"
    }

    @JvmStatic
    fun Vector3f.toRGB() = toRGB(255)
    @JvmStatic
    fun Vector3f.toRGB(scale: Int): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                (255 shl 24)
    }

    @JvmStatic
    fun Vector4f.toARGB() = toARGB(255)
    @JvmStatic
    fun Vector4f.toARGB(scale: Int): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                clamp((w * 255).toInt(), 0, 255).shl(24)
    }

    @JvmStatic
    fun Vector3d.toRGB() = toRGB(255)
    @JvmStatic
    fun Vector3d.toRGB(scale: Int): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                (255 shl 24)
    }

    @JvmStatic
    fun Vector4d.toARGB() = toARGB(255)
    @JvmStatic
    fun Vector4d.toARGB(scale: Int): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                clamp((w * 255).toInt(), 0, 255).shl(24)
    }

}
