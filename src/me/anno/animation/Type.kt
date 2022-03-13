package me.anno.animation

import me.anno.maths.Maths.clamp
import me.anno.utils.types.Casting.castToDouble
import me.anno.utils.types.Casting.castToDouble2
import me.anno.utils.types.Casting.castToFloat
import me.anno.utils.types.Casting.castToFloat2
import me.anno.utils.types.Casting.castToInt
import me.anno.utils.types.Casting.castToInt2
import me.anno.utils.types.Casting.castToLong
import me.anno.utils.types.Casting.castToString
import me.anno.utils.types.Casting.castToVector2d
import me.anno.utils.types.Casting.castToVector2f
import me.anno.utils.types.Casting.castToVector3d
import me.anno.utils.types.Casting.castToVector3f
import me.anno.utils.types.Casting.castToVector4d
import me.anno.utils.types.Casting.castToVector4f
import org.joml.*
import kotlin.math.max

class Type(
    defaultValue: Any,
    val components: Int,
    val unitScale: Float,
    val hasLinear: Boolean,
    val hasExponential: Boolean,
    val clampFunc: ((Any?) -> Any)?,
    val acceptOrNull: (Any) -> Any?
) {

    constructor(defaultValue: Any, clampFunc: ((Any?) -> Any)?, acceptOrNull: (Any) -> Any?) :
            this(defaultValue, 1, 1f, true, true, clampFunc, acceptOrNull)

    constructor(defaultValue: Any, components: Int) :
            this(defaultValue, components, 1f, true, true, { it!! }, { it })

    val defaultValue = acceptOrNull(clamp(defaultValue))
        ?: throw IllegalArgumentException("Incompatible default value $defaultValue")

    override fun toString() = "Type[${defaultValue.javaClass.simpleName} x $components]"

    fun withDefaultValue(defaultValue: Any) = Type(
        defaultValue, components, unitScale, hasLinear, hasExponential,
        clampFunc, acceptOrNull
    )

    fun withDefault(defaultValue: Any) = withDefaultValue(defaultValue)

    @Suppress("UNCHECKED_CAST")
    fun <V> clamp(value: V): V = if (clampFunc != null) clampFunc.invoke(value) as V else value

    companion object {

        val ANY = Type(0, 16, 1f, true, true, null) { it }
        val INT = Type(0, 1, 1f, true, true, null, ::castToInt)
        val INT_PLUS = Type(0, 1, 1f, true, true, { max(castToInt2(it), 0) }, ::castToInt)
        val LONG = Type(0L, 1, 1f, true, true, null, ::castToLong)
        val FLOAT = Type(0f, 1, 1f, true, true, null, ::castToFloat)
        val FLOAT_01 = Type(0f, 1, 1f, true, true, { clamp(castToFloat2(it), 0f, 1f) }, ::castToFloat)
        val FLOAT_03 = Type(0f, 1, 1f, true, true, { clamp(castToFloat2(it), 0f, 3f) }, ::castToFloat)
        val FLOAT_01_EXP = Type(0f, 1, 1f, false, true, { clamp(castToFloat2(it), 0f, 1f) }, ::castToFloat)
        val FLOAT_PLUS = Type(0f, 1, 1f, true, true, { max(castToFloat2(it), 0f) }, ::castToFloat)
        val FLOAT_PLUS_EXP = Type(0f, 1, 1f, false, true, { max(castToFloat2(it), 0f) }, ::castToFloat)
        val FLOAT_PERCENT = Type(100f, 1, 100f, true, false, { clamp(castToFloat2(it), 0f, 100f) }, ::castToFloat)
        val ANGLE = Type(0f, 1, 90f, true, false, null, ::castToFloat)
        val DOUBLE = Type(0.0, 1, 1f, true, true, null, ::castToDouble)
        val DOUBLE_PLUS = Type(0.0, 1, 1f, true, true, { max(castToDouble2(it), 0.0) }, ::castToDouble)
        val VEC2 = Type(Vector2f(), 2, 1f, true, true, null, ::castToVector2f)
        val VEC2_PLUS = Type(Vector2f(), 2, 1f, true, true, ::positiveVector2f, ::castToVector2f)
        val VEC3 = Type(Vector3f(), 3, 1f, true, true, null, ::castToVector3f)
        val VEC4 = Type(Vector4f(), 4, 1f, true, true, null, ::castToVector4f)
        val VEC4_PLUS = Type(Vector4f(), 4, 1f, true, true, {
            when (it) {
                is Float -> max(it, 0f)
                is Double -> max(it, 0.0)
                is Vector4f -> Vector4f(max(it.x, 0f), max(it.y, 0f), max(it.z, 0f), max(it.w, 0f))
                else -> throw RuntimeException("Unsupported type $it")
            }
        }, ::castToVector4f)
        val POSITION = Type(Vector3f(), 3, 1f, true, true, null, ::castToVector3f)
        val POSITION_2D = Type(Vector2f(), 2, 1f, true, true, null, ::castToVector2f)
        val SCALE = Type(Vector3f(1f, 1f, 1f), 3, 1f, true, true, null, ::castToVector3f)
        val ROT_YXZ = Type(Vector3f(), 3, 90f, true, true, null, ::castToVector3f)
        val ROT_YXZ64 = Type(Vector3d(), 3, 90f, true, true, null, ::castToVector3d)
        val ROT_Y = Type(0f, 1, 90f, true, true, null, ::castToFloat)
        val ROT_XZ = Type(Vector3f(), 2, 90f, true, true, null, ::castToVector2f)
        val SKEW_2D = Type(Vector2f(), 2, 1f, true, true, null, ::castToVector2f)
        val QUATERNION =
            Type(Quaternionf(), 4, 1f, true, true, null) { if (it is Quaternionfc || it is Quaterniondc) it else null }
        val QUATERNIOND =
            Type(Quaterniond(), 4, 1f, true, true, null) { if (it is Quaternionfc || it is Quaterniondc) it else null }
        val COLOR = Type(Vector4f(1f, 1f, 1f, 1f), 4, 1f, true, true, {
            when (it) {
                is Vector4f -> {
                    it.x = clamp(it.x, 0f, 1f)
                    it.y = clamp(it.y, 0f, 1f)
                    it.z = clamp(it.z, 0f, 1f)
                    it.w = clamp(it.w, 0f, 1f)
                    it
                }
                is Float -> clamp(it, 0f, 1f)
                is Double -> clamp(it, 0.0, 1.0)
                else -> throw RuntimeException()
            }
        }, ::castToVector4f)
        val COLOR3 = Type(Vector3f(1f, 1f, 1f), 3, 1f, true, true, {
            when (it) {
                is Vector3f -> {
                    it.x = clamp(it.x, 0f, 1f)
                    it.y = clamp(it.y, 0f, 1f)
                    it.z = clamp(it.z, 0f, 1f)
                    it
                }
                is Float -> clamp(it, 0f, 1f)
                else -> throw RuntimeException()
            }
        }, ::castToVector3f)

        val TILING = Type(Vector4f(1f, 1f, 0f, 0f), 4, 1f, true, true, null, ::castToVector4f)

        /**
         * constant rate factor, 0 = lossless, 51 = worst, 23 = default
         * https://trac.ffmpeg.org/wiki/Encode/H.264
         * */
        val VIDEO_QUALITY_CRF = Type(23, 1, 1f, true, false, { clamp(it as Int, 0, 51) }, ::castToInt)

        val VEC2D = Type(Vector2d(), 2, 1f, true, true, null, ::castToVector2d)
        val VEC3D = Type(Vector3d(), 3, 1f, true, true, null, ::castToVector3d)
        val VEC4D = Type(Vector4d(), 4, 1f, true, true, null, ::castToVector4d)

        val STRING = Type("", 1, 1f, false, false, { castToString(it).replace("\r", "") }, ::castToString)
        val ALIGNMENT = Type(0f, 1, 4f, true, false, { clamp(castToFloat2(it), -1f, +1f) }, ::castToFloat)

        private fun positiveVector2f(any: Any?): Any {
            return castToVector2f(any ?: 0f)?.apply {
                x = max(x, 0f)
                y = max(y, 0f)
            } ?: Vector2f()
        }

    }

}