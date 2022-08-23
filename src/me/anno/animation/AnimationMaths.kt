package me.anno.animation

import me.anno.utils.strings.StringMixer
import org.joml.*
import kotlin.math.roundToInt

object AnimationMaths {

    // to save allocations
    fun v2(dst: Any?) = dst as? Vector2f ?: Vector2f()
    fun v3(dst: Any?) = dst as? Vector3f ?: Vector3f()
    fun v4(dst: Any?) = dst as? Vector4f ?: Vector4f()
    fun q4(dst: Any?) = dst as? Quaternionf ?: Quaternionf()

    /**
     * b + a * f
     * */
    fun mulAdd(first: Any, second: Any, f: Double, dst: Any?): Any {
        return when (first) {
            is Float -> first + (second as Float) * f.toFloat()
            is Double -> first + (second as Double) * f
            is Vector2f -> {
                second as Vector2f
                val g = f.toFloat()
                val result = v2(dst)
                result.set(
                    first.x + second.x * g,
                    first.y + second.y * g
                )
            }
            is Vector3f -> {
                second as Vector3f
                val g = f.toFloat()
                val result = v3(dst)
                result.set(
                    first.x + second.x * g,
                    first.y + second.y * g,
                    first.z + second.z * g
                )
            }
            is Vector4f -> {
                second as Vector4f
                val g = f.toFloat()
                val result = v4(dst)
                result.set(
                    first.x + second.x * g,
                    first.y + second.y * g,
                    first.z + second.z * g,
                    first.w + second.w * g
                )
            }
            is String -> StringMixer.mix(first.toString(), second.toString(), f)
            else -> throw RuntimeException("don't know how to mul-add $second and $first")
        }
    }

    /**
     * a * f
     * */
    fun mul(a: Any, f: Double, dst: Any?): Any {
        return when (a) {
            is Int -> a * f
            is Long -> a * f
            is Float -> a * f.toFloat()
            is Double -> a * f
            is Vector2f -> v2(dst).set(a).mul(f.toFloat())
            is Vector3f -> v3(dst).set(a).mul(f.toFloat())
            is Vector4f -> v4(dst).set(a).mul(f.toFloat())
            is String -> a//a.substring(0, clamp((a.length * f).roundToInt(), 0, a.length))
            else -> throw RuntimeException("don't know how to mul $a")
        }
    }


    /**
     * a * (1-f) + f * b
     * */
    fun mix(a: Any?, b: Any?, f: Double, type: Type): Any? {
        val g = 1.0 - f
        return when (type) {
            Type.INT,
            Type.INT_PLUS -> ((a as Int) * g + f * (b as Int)).roundToInt()
            Type.LONG -> ((a as Long) * g + f * (b as Long)).toLong()
            Type.FLOAT,
            Type.FLOAT_01, Type.FLOAT_01_EXP,
            Type.FLOAT_PLUS -> ((a as Float) * g + f * (b as Float)).toFloat()
            Type.DOUBLE -> (a as Double) * g + f * (b as Double)
            Type.SKEW_2D -> (a as Vector2f).lerp(b as Vector2f, f.toFloat(), Vector2f())
            Type.POSITION,
            Type.ROT_YXZ,
            Type.SCALE -> (a as Vector3f).lerp(b as Vector3f, f.toFloat(), Vector3f())
            Type.COLOR, Type.TILING -> (a as Vector4f).lerp(b as Vector4f, f.toFloat(), Vector4f())
            Type.QUATERNION -> (a as Quaternionf).slerp(b as Quaternionf, f.toFloat())
            Type.STRING -> StringMixer.mix(a.toString(), b.toString(), f)
            else -> throw RuntimeException("don't know how to linearly interpolate $a and $b")
        }
    }

}