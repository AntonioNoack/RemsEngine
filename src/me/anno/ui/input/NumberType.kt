package me.anno.ui.input

import me.anno.maths.Maths
import me.anno.utils.types.Casting
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import kotlin.math.max

class NumberType(
    defaultValue: Any,
    val numComponents: Int,
    val unitScale: Float,
    val hasLinear: Boolean,
    val hasExponential: Boolean,
    val clampFunc: ((Any?) -> Any)?,
    val acceptOrNull: (Any?) -> Any?
) {

    constructor(defaultValue: Any, clampFunc: ((Any?) -> Any)?, acceptOrNull: (Any?) -> Any?) :
            this(defaultValue, 1, 1f, true, true, clampFunc, acceptOrNull)

    constructor(defaultValue: Any, components: Int) :
            this(defaultValue, components, 1f, true, true, { it!! }, { it })

    val defaultValue = acceptOrNull(clamp(defaultValue))
        ?: throw IllegalArgumentException("Incompatible default value $defaultValue")

    override fun toString() = "Type[${defaultValue::class.simpleName} x $numComponents]"

    fun withDefaultValue(defaultValue: Any): NumberType = NumberType(
        defaultValue, numComponents, unitScale, hasLinear, hasExponential,
        clampFunc, acceptOrNull
    )

    fun withDefault(defaultValue: Any): NumberType = withDefaultValue(defaultValue)

    fun clamp(value: Any): Any = clampFunc?.invoke(value) ?: value

    @Suppress("unused")
    companion object {

        val ANY = NumberType(
            0, 16, 1f,
            hasLinear = true, hasExponential = true, clampFunc = null
        ) { it }

        val INT = NumberType(
            0, 1, 1f, hasLinear = true, hasExponential = true,
            null, Casting::castToInt
        )

        val INT_PLUS = NumberType(
            0, 1, 1f, hasLinear = true, hasExponential = true,
            { max(Casting.castToInt2(it), 0) }, Casting::castToInt
        )

        val LONG = NumberType(
            0L, 1, 1f, hasLinear = true, hasExponential = true,
            null, Casting::castToLong
        )

        val LONG_PLUS = NumberType(
            0, 1, 1f, hasLinear = true, hasExponential = true,
            { max(Casting.castToLong2(it), 0) }, Casting::castToLong
        )

        val FLOAT = NumberType(
            0f, 1, 1f, hasLinear = true, hasExponential = true,
            null, Casting::castToFloat
        )

        val FLOAT_01 = NumberType(
            0f, 1, 1f, hasLinear = true, hasExponential = true,
            { Maths.clamp(Casting.castToFloat2(it), 0f, 1f) }, Casting::castToFloat
        )

        val FLOAT_03 = NumberType(
            0f, 1, 1f, hasLinear = true, hasExponential = true,
            { Maths.clamp(Casting.castToFloat2(it), 0f, 3f) }, Casting::castToFloat
        )

        val FLOAT_01_EXP = NumberType(
            0f, 1, 1f, false, hasExponential = true,
            { Maths.clamp(Casting.castToFloat2(it), 0f, 1f) }, Casting::castToFloat
        )

        val FLOAT_PLUS = NumberType(
            0f, 1, 1f, true, hasExponential = true,
            { max(Casting.castToFloat2(it), 0f) }, Casting::castToFloat
        )

        val FLOAT_PLUS_EXP = NumberType(
            0f, 1, 1f, false, hasExponential = true,
            { max(Casting.castToFloat2(it), 0f) }, Casting::castToFloat
        )

        val FLOAT_PERCENT = NumberType(
            100f, 1, 100f, true, hasExponential = false,
            { Maths.clamp(Casting.castToFloat2(it), 0f, 100f) }, Casting::castToFloat
        )

        val ANGLE = NumberType(
            0f, 1, 90f,
            true, hasExponential = false, null, Casting::castToFloat
        )

        val DOUBLE = NumberType(
            0.0, 1, 1f,
            true, hasExponential = true, null, Casting::castToDouble
        )

        val DOUBLE_PLUS = NumberType(
            0.0, 1, 1f, true, hasExponential = true,
            { max(Casting.castToDouble2(it), 0.0) }, Casting::castToDouble
        )

        val VEC2 = NumberType(
            Vector2f(), 2, 1f,
            true, hasExponential = true, null, Casting::castToVector2f
        )

        val VEC2_PLUS = NumberType(
            Vector2f(), 2, 1f,
            true, hasExponential = true, ::positiveVector2f, Casting::castToVector2f
        )

        val VEC3 = NumberType(Vector3f(), 3, 1f, true, hasExponential = true, null, Casting::castToVector3f)
        val VEC4 = NumberType(Vector4f(), 4, 1f, true, hasExponential = true, null, Casting::castToVector4f)
        val PLANE4 = NumberType(Planef(), 4, 1f, true, hasExponential = true, null, Casting::castToPlanef)
        val PLANE4D = NumberType(Planed(), 4, 1f, true, hasExponential = true, null, Casting::castToPlaned)
        val VEC4_PLUS = NumberType(Vector4f(), 4, 1f, true, hasExponential = true, {
            when (it) {
                is Float -> max(it, 0f)
                is Double -> max(it, 0.0)
                is Vector -> {
                    for (i in 0 until it.numComponents) {
                        it.setComp(i, max(it.getComp(i), 0.0))
                    }
                    it
                }
                else -> throw RuntimeException("Unsupported type $it")
            }
        }, Casting::castToVector4f)
        val POSITION = NumberType(Vector3f(), 3, 1f, true, hasExponential = true, null, Casting::castToVector3f)
        val POSITION_2D = NumberType(Vector2f(), 2, 1f, true, hasExponential = true, null, Casting::castToVector2f)
        val SCALE = NumberType(Vector3f(1f, 1f, 1f), 3, 1f, true, hasExponential = true, null, Casting::castToVector3f)
        val ROT_YXZ = NumberType(Vector3f(), 3, 90f, true, hasExponential = true, null, Casting::castToVector3f)
        val ROT_YXZ64 = NumberType(Vector3d(), 3, 90f, true, hasExponential = true, null, Casting::castToVector3d)
        val ROT_Y = NumberType(0f, 1, 90f, true, hasExponential = true, null, Casting::castToFloat)
        val ROT_XZ = NumberType(Vector3f(), 2, 90f, true, hasExponential = true, null, Casting::castToVector2f)
        val SKEW_2D = NumberType(Vector2f(), 2, 1f, true, hasExponential = true, null, Casting::castToVector2f)

        val QUATERNION = NumberType(
            Quaternionf(), 4, 1f, true, hasExponential = true, null
        ) { if (it is Quaternionf || it is Quaterniond) it else null }

        val QUATERNIOND = NumberType(
            Quaterniond(), 4, 1f, true, hasExponential = true, null
        ) { if (it is Quaternionf || it is Quaterniond) it else null }

        val COLOR = NumberType(
            Vector4f(1f, 1f, 1f, 1f), 4, 1f, true,
            hasExponential = true, {
                when (it) {
                    is Vector -> {
                        for (i in 0 until it.numComponents) {
                            it.setComp(i, Maths.clamp(it.getComp(i)))
                        }
                        it
                    }
                    is Float -> Maths.clamp(it)
                    is Double -> Maths.clamp(it)
                    else -> throw RuntimeException()
                }
            }, Casting::castToVector4f
        )

        val COLOR3 = NumberType(
            Vector3f(1f, 1f, 1f), 3, 1f, true, hasExponential = true,
            COLOR.clampFunc, Casting::castToVector3f
        )

        val TILING = NumberType(
            Vector4f(1f, 1f, 0f, 0f), 4, 1f,
            true, hasExponential = true, null, Casting::castToVector4f
        )

        /**
         * constant rate factor, 0 = lossless, 51 = worst, 23 = default
         * https://trac.ffmpeg.org/wiki/Encode/H.264
         * */
        val VIDEO_QUALITY_CRF = NumberType(
            23, 1, 1f, true, hasExponential = false,
            { Maths.clamp(Casting.castToInt2(it), 0, 51) }, Casting::castToInt
        )

        val VEC2D = NumberType(Vector2d(), 2, 1f, true, hasExponential = true, null, Casting::castToVector2d)
        val VEC3D = NumberType(Vector3d(), 3, 1f, true, hasExponential = true, null, Casting::castToVector3d)
        val VEC4D = NumberType(Vector4d(), 4, 1f, true, hasExponential = true, null, Casting::castToVector4d)

        val STRING = NumberType(
            "", 1, 1f, false, hasExponential = false,
            { Casting.castToString(it).replace("\r", "") }, Casting::castToString
        )

        val ALIGNMENT = NumberType(
            0f, 1, 4f, true, hasExponential = false,
            { Maths.clamp(Casting.castToFloat2(it), -1f, +1f) }, Casting::castToFloat
        )

        private fun positiveVector2f(any: Any?): Any {
            return Casting.castToVector2f(any ?: 0f)?.apply {
                x = max(x, 0f)
                y = max(y, 0f)
            } ?: Vector2f()
        }
    }
}