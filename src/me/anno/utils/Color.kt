package me.anno.utils

import me.anno.ecs.annotations.Docs
import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object Color {

    const val black = 0xff000000.toInt()
    const val white = 0xffffff or black

    @JvmField
    val black4 = Vector4f(0f)

    @JvmField
    val black3 = Vector3f(0f)

    @JvmField
    val black2 = Vector2f(0f)

    @JvmField
    val white4 = Vector4f(1f)

    @JvmField
    val white3 = Vector3f(1f)

    @JvmField
    val white2 = Vector2f(1f)

    @JvmStatic
    @Docs("Returns the red value between 0 and 255")
    fun Int.r() = shr(16) and 255

    @JvmStatic
    @Docs("Returns the green value between 0 and 255")
    fun Int.g() = shr(8) and 255

    @JvmStatic
    @Docs("Returns the blue value between 0 and 255")
    fun Int.b() = this and 255

    @JvmStatic
    @Docs("Returns the alpha value between 0 and 255")
    fun Int.a() = ushr(24)

    @JvmStatic
    @Docs("Returns the red value between 0 and 1")
    fun Int.r01() = (shr(16) and 255) / 255f

    @JvmStatic
    @Docs("Returns the green value between 0 and 1")
    fun Int.g01() = (shr(8) and 255) / 255f

    @JvmStatic
    @Docs("Returns the blue value between 0 and 1")
    fun Int.b01() = (this and 255) / 255f

    @JvmStatic
    @Docs("Returns the alpha value between 0 and 1")
    fun Int.a01() = ushr(24) / 255f

    @JvmStatic
    @Docs("Sets the alpha of the ARGB color, alpha from 0 to 1; crashes for NaN")
    fun Int.withAlpha(alpha: Float): Int = rgba(r(), g(), b(), (255f * alpha).roundToInt())

    @JvmStatic
    @Docs("Multiplies the alpha of the ARGB color, alpha from 0 to 1; crashes for NaN")
    fun Int.mulAlpha(alpha: Float): Int = rgba(r(), g(), b(), (a() * alpha).roundToInt())

    @JvmStatic
    @Docs("Sets the alpha of the ARGB color, alpha from 0 to 255")
    fun Int.withAlpha(alpha: Int): Int = rgba(r(), g(), b(), alpha)

    @JvmStatic
    fun Int.mulARGB(other: Int): Int {
        return mulChannel(this, other, 24) or
                mulChannel(this, other, 16) or
                mulChannel(this, other, 8) or
                mulChannel(this, other, 0)
    }

    @JvmStatic
    @Docs("Creates an ARGB color from r,g,b bytes, and sets alpha to 255")
    fun rgb(r: Byte, g: Byte, b: Byte): Int =
        r.toInt().and(255).shl(16) or
                g.toInt().and(255).shl(8) or
                b.toInt().and(255) or
                black

    @JvmStatic
    @Docs("Creates an ARGB color from r,g,b integers, and sets alpha to 255; clamps values")
    fun rgb(r: Int, g: Int, b: Int): Int =
        clamp(r, 0, 255).shl(16) or
                clamp(g, 0, 255).shl(8) or
                clamp(b, 0, 255) or
                black

    @JvmStatic
    @Docs("Creates an ARGB color from r,g,b floats, and sets alpha to 1; crashes for NaNs")
    fun rgb(r: Float, g: Float, b: Float): Int =
        rgb((r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())

    @JvmStatic
    @Docs("Creates an ARGB color from r,g,b,a bytes")
    fun rgba(r: Byte, g: Byte, b: Byte, a: Byte): Int = r.toInt().and(255).shl(16) or
            g.toInt().and(255).shl(8) or
            b.toInt().and(255) or
            a.toInt().and(255).shl(24)

    @JvmStatic
    @Docs("Creates an ARGB color from r,g,b,a integers; clamps values")
    fun rgba(r: Int, g: Int, b: Int, a: Int): Int =
        clamp(r, 0, 255).shl(16) or
                clamp(g, 0, 255).shl(8) or
                clamp(b, 0, 255) or
                clamp(a, 0, 255).shl(24)

    @JvmStatic
    @Docs("Creates an ARGB color from r,g,b,a integers; clamps values; crashes on NaNs")
    fun rgba(r: Float, g: Float, b: Float, a: Float): Int =
        rgba((r * 255f).roundToInt(), (g * 255f).roundToInt(), (b * 255f).roundToInt(), (a * 255f).roundToInt())

    @JvmStatic
    @Docs("Creates an ARGB color from a,r,g,b integers; clamps values")
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

    @Suppress("SpellCheckingInspection")
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
    fun Int.toHexString(): String {
        return hex32(this)
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
    fun Vector3f.toRGB(scale: Int = 255): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                (255 shl 24)
    }

    @JvmStatic
    fun Vector4f.toARGB(scale: Int = 255): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                clamp((w * 255).toInt(), 0, 255).shl(24)
    }

    @JvmStatic
    fun Vector3d.toRGB(scale: Int = 255): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                (255 shl 24)
    }

    @JvmStatic
    fun Vector4d.toARGB(scale: Int = 255): Int {
        return clamp((x * scale).toInt(), 0, 255).shl(16) or
                clamp((y * scale).toInt(), 0, 255).shl(8) or
                clamp((z * scale).toInt(), 0, 255) or
                clamp((w * 255).toInt(), 0, 255).shl(24)
    }

    @JvmStatic
    fun convertARGB2RGBA(i: Int): Int {
        return i.ushr(24) or i.shl(8)
    }

    @JvmStatic
    fun convertRGBA2ARGB(i: Int): Int {
        return i.ushr(8) or i.shl(24)
    }

    @JvmStatic
    fun convertABGR2ARGB(i: Int): Int {
        return i.and(0xff00ff00.toInt()) or i.shr(16).and(0xff) or i.and(0xff).shl(16)
    }

    @JvmStatic
    fun mixChannel(a: Int, b: Int, shift: Int, f: Float): Int {
        return Maths.mix((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel(a: Int, b: Int, shift: Int, f: Int): Int {
        return Maths.mix((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel2(a: Int, b: Int, shift: Int, f: Int): Int {
        return Maths.mix2((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel2(a: Int, b: Int, shift: Int, f: Float): Int {
        return Maths.mix2((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
    }

    @JvmStatic
    fun mixChannel22d(v00: Int, v01: Int, v10: Int, v11: Int, shift: Int, fx: Float, fy: Float): Int {
        val r00 = Maths.sq((v00 shr shift) and 255).toFloat()
        val r01 = Maths.sq((v01 shr shift) and 255).toFloat()
        val r10 = Maths.sq((v10 shr shift) and 255).toFloat()
        val r11 = Maths.sq((v11 shr shift) and 255).toFloat()
        return sqrt(Maths.mix2d(r00, r01, r10, r11, fx, fy)).roundToInt() shl shift
    }

    @JvmStatic
    fun mixChannelRandomly(a: Int, b: Int, shift: Int, f: Float): Int {
        val ai = (a shr shift) and 0xff
        val bi = (b shr shift) and 0xff
        return clamp(Maths.mixRandomly(ai, bi, f), 0, 255) shl shift
    }

    @JvmStatic
    fun mixARGB(a: Int, b: Int, f: Float): Int {
        return mixChannel(a, b, 24, f) or
                mixChannel(a, b, 16, f) or
                mixChannel(a, b, 8, f) or
                mixChannel(a, b, 0, f)
    }

    @JvmStatic
    fun mixARGB(a: Int, b: Int, f: Int): Int {
        return mixChannel(a, b, 24, f) or
                mixChannel(a, b, 16, f) or
                mixChannel(a, b, 8, f) or
                mixChannel(a, b, 0, f)
    }

    fun mulChannel(a: Int, b: Int, sh: Int): Int {
        return (a.ushr(sh).and(0xff) * b.ushr(sh).and(0xff) + 128).ushr(8).shl(sh)
    }

    @JvmStatic
    fun mixARGB2(a: Int, b: Int, f: Float): Int {
        return mixChannel2(a, b, 24, f) or
                mixChannel2(a, b, 16, f) or
                mixChannel2(a, b, 8, f) or
                mixChannel2(a, b, 0, f)
    }

    @JvmStatic
    fun mixARGB22d(v00: Int, v01: Int, v10: Int, v11: Int, fx: Float, fy: Float): Int {
        return mixChannel22d(v00, v01, v10, v11, 24, fx, fy) or
                mixChannel22d(v00, v01, v10, v11, 16, fx, fy) or
                mixChannel22d(v00, v01, v10, v11, 8, fx, fy) or
                mixChannel22d(v00, v01, v10, v11, 0, fx, fy)
    }

    @JvmStatic
    fun mixARGB2(a: Int, b: Int, f: Int): Int {
        return mixChannel2(a, b, 24, f) or
                mixChannel2(a, b, 16, f) or
                mixChannel2(a, b, 8, f) or
                mixChannel2(a, b, 0, f)
    }

    @JvmStatic
    fun mixARGBRandomly(a: Int, b: Int, f: Float): Int {
        return mixChannelRandomly(a, b, 24, f) or
                mixChannelRandomly(a, b, 16, f) or
                mixChannelRandomly(a, b, 8, f) or
                mixChannelRandomly(a, b, 0, f)
    }
}
