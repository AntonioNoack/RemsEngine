package me.anno.objects.animation

import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class Type(
    val defaultValue: Any,
    val components: Int,
    val unitScale: Float,
    val hasLinear: Boolean,
    val hasExponential: Boolean,
    val minValue: Any?, val maxValue: Any?,
    val accepts: (Any?) -> Boolean
) {

    fun <V> clamp(value: V): V {
        if (minValue != null || maxValue != null) {
            value as Comparable<V>
            if (minValue != null && value < minValue as V) return minValue
            if (maxValue != null && value >= maxValue as V) return maxValue
        }
        return value
    }

    companion object {

        val ANY = Type(0, 16, 1f, true, true, null, null) { true }
        val INT = Type(0, 1, 1f, true, true, null, null){ it is Int }
        val INT_PLUS = Type(0, 1, 1f, true, true, 0, null){ it is Int }
        val LONG = Type(0L, 1, 1f, true, true, null, null) { it is Long }
        val FLOAT = Type(0f, 1, 1f, true, true, null, null) { it is Float }
        val FLOAT_01 = Type(0f, 1, 1f, true, true, 0f, 1f) { it is Float }
        val FLOAT_01_EXP = Type(0f, 1, 1f, false, true, 0f, 1f) { it is Float }
        val FLOAT_PLUS = Type(0f, 1, 1f, true, true, 0f, null) { it is Float }
        val FLOAT_PLUS_EXP = Type(0f, 1, 1f, false, true, 0f, null) { it is Float }
        val FLOAT_PERCENT = Type(100f, 1, 100f, true, false, 0f, 100f) { it is Float }
        val DOUBLE = Type(0.0, 1, 1f, true, true, null, null) { it is Double }
        val VEC2 = Type(Vector2f(), 2, 1f, true, true, null, null) { it is Vector2f }
        val VEC3 = Type(Vector3f(), 3, 1f, true, true, null, null) { it is Vector3f }
        val VEC4 = Type(Vector4f(), 4, 1f, true, true, null, null) { it is Vector4f }
        val POSITION = Type(Vector3f(), 3, 1f, true, true, null, null) { it is Vector3f }
        val SCALE = Type(Vector3f(1f, 1f, 1f), 3, 1f, true, true, null, null) { it is Vector3f }
        val ROT_YXZ = Type(Vector3f(), 3, 90f, true, true, null, null) { it is Vector3f }
        val SKEW_2D = Type(Vector2f(), 2, 1f, true, true, null, null) { it is Vector2f }
        val QUATERNION = Type(Quaternionf(), 4, 1f, true, true, null, null) { it is Quaternionf }
        val COLOR = Type(Vector4f(1f, 1f, 1f, 1f), 4, 1f, true, true, null, null) { it is Vector4f }
        val COLOR3 = Type(Vector3f(1f, 1f, 1f), 3, 1f, true, true, null, null) { it is Vector3f }
        val TILING = Type(Vector4f(1f, 1f, 0f, 0f), 4, 1f, true, true, null, null) { it is Vector4f }

        /**
         * constant rate factor, 0 = lossless, 51 = worst, 23 = default
         * https://trac.ffmpeg.org/wiki/Encode/H.264
         * */
        val VIDEO_QUALITY_CRF = Type(23, 1, 1f, true, false, 0, 51){ it is Int }

    }

}