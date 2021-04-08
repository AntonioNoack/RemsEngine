package me.anno.objects.animation

import me.anno.config.DefaultStyle.black3
import me.anno.gpu.GFX.glThread
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.AnimationMaths.mul
import me.anno.objects.animation.AnimationMaths.mulAdd
import me.anno.objects.animation.Interpolation.Companion.getWeights
import me.anno.objects.animation.TimeValue.Companion.writeValue
import me.anno.objects.animation.drivers.AnimationDriver
import me.anno.studio.rems.RemsStudio.root
import me.anno.utils.Maths.clamp
import me.anno.utils.WrongClassType
import me.anno.utils.strings.StringMixer
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.Vectors.plus
import me.anno.utils.types.Vectors.times
import org.apache.logging.log4j.LogManager
import org.joml.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AnimatedProperty<V>(var type: Type, var defaultValue: V) : Saveable() {

    constructor(type: Type) : this(type, type.defaultValue as V)

    companion object {

        private val LOGGER = LogManager.getLogger(AnimatedProperty::class)

        fun any() = AnimatedProperty<Any>(Type.ANY)
        fun int() = AnimatedProperty<Int>(Type.INT)
        fun int(defaultValue: Int) = AnimatedProperty(Type.INT, defaultValue)
        fun intPlus() = AnimatedProperty<Int>(Type.INT_PLUS)
        fun intPlus(defaultValue: Int) = AnimatedProperty(Type.INT_PLUS, defaultValue)
        fun long() = AnimatedProperty<Long>(Type.LONG)
        fun float() = AnimatedProperty<Float>(Type.FLOAT)
        fun float(defaultValue: Float) = AnimatedProperty(Type.FLOAT, defaultValue)
        fun floatPlus() = AnimatedProperty<Float>(Type.FLOAT_PLUS)
        fun floatPlus(defaultValue: Float) = AnimatedProperty(Type.FLOAT_PLUS, defaultValue)
        fun floatPlusExp() = AnimatedProperty<Float>(Type.FLOAT_PLUS_EXP)
        fun floatPlusExp(defaultValue: Float) = AnimatedProperty(Type.FLOAT_PLUS_EXP, defaultValue)
        fun float01() = AnimatedProperty<Float>(Type.FLOAT_01)
        fun float01(defaultValue: Float) = AnimatedProperty(Type.FLOAT_01, defaultValue)
        fun float01exp(defaultValue: Float) = AnimatedProperty(Type.FLOAT_01_EXP, defaultValue)
        fun floatPercent() = AnimatedProperty<Float>(Type.FLOAT_PERCENT)
        fun double() = AnimatedProperty<Double>(Type.DOUBLE)
        fun double(defaultValue: Double) = AnimatedProperty(Type.DOUBLE, defaultValue)
        fun vec2() = AnimatedProperty<Vector2f>(Type.VEC2)
        fun vec2(defaultValue: Vector2fc) = AnimatedProperty(Type.VEC2, defaultValue)
        fun vec3() = AnimatedProperty(Type.VEC3, black3)
        fun dir3() = vec3(Vector3f(0f, 1f, 0f))
        fun vec3(defaultValue: Vector3fc) = AnimatedProperty(Type.VEC3, defaultValue)
        fun vec4() = AnimatedProperty<Vector4fc>(Type.VEC4)
        fun vec4(defaultValue: Vector4fc) = AnimatedProperty(Type.VEC4, defaultValue)
        fun pos() = AnimatedProperty<Vector3f>(Type.POSITION)
        fun pos(defaultValue: Vector3fc) = AnimatedProperty(Type.POSITION, defaultValue)
        fun pos2D() = AnimatedProperty<Vector2fc>(Type.POSITION_2D)
        fun rotYXZ() = AnimatedProperty<Vector3fc>(Type.ROT_YXZ)
        fun rotY() = AnimatedProperty<Float>(Type.ROT_Y)
        fun rotXZ() = AnimatedProperty<Vector2fc>(Type.ROT_XZ)
        fun scale() = AnimatedProperty<Vector3fc>(Type.SCALE)
        fun scale(defaultValue: Vector3fc) = AnimatedProperty(Type.SCALE, defaultValue)
        fun color() = AnimatedProperty<Vector4fc>(Type.COLOR)
        fun color(defaultValue: Vector4fc) = AnimatedProperty(Type.COLOR, defaultValue)
        fun color3() = AnimatedProperty<Vector3fc>(Type.COLOR3)
        fun color3(defaultValue: Vector3fc) = AnimatedProperty(Type.COLOR3, defaultValue)
        fun quat() = AnimatedProperty<Quaternionf>(Type.QUATERNION)
        fun skew() = AnimatedProperty<Vector2fc>(Type.SKEW_2D)
        fun tiling() = AnimatedProperty<Vector4fc>(Type.TILING)

        fun string() = AnimatedProperty(Type.STRING, "")
        fun alignment() = AnimatedProperty(Type.ALIGNMENT, 0f)

        // fun <V> set() = AnimatedProperty(Type.ANY, emptySet<V>())

    }

    val drivers = arrayOfNulls<AnimationDriver>(type.components)

    var isAnimated = false
    val keyframes = ArrayList<Keyframe<V>>()

    fun ensureCorrectType(v: Any?): V {
        return type.acceptOrNull(v!!) as V ?: throw RuntimeException("got $v for $type")
    }

    fun clampAny(value: Any) = clamp(value as V)
    fun clamp(value: V): V {
        return type.clampFunc?.invoke(value) as V ?: value
    }

    fun set(value: V): AnimatedProperty<V> {
        synchronized(this) {
            checkThread()
            keyframes.clear()
            keyframes.add(Keyframe(0.0, clamp(value)))
            keyframes.sort()
            return this
        }
    }

    fun addKeyframe(time: Double, value: Any) =
        addKeyframe(time, value, 0.001)

    fun addKeyframe(time: Double, value: Any, equalityDt: Double): Keyframe<V>? {
        val value2 = type.acceptOrNull(value)
        return if (value2 != null) {
            addKeyframeInternal(time, clamp(value2 as V), equalityDt)
        } else {
            LOGGER.warn("Value $value is not accepted by type $type!")
            null
        }
    }

    fun checkIsAnimated() {
        isAnimated = keyframes.size >= 2 || drivers.any { it != null }
    }

    private fun addKeyframeInternal(time: Double, value: V, equalityDt: Double): Keyframe<V> {
        synchronized(this) {
            checkThread()
            ensureCorrectType(value)
            if (isAnimated) {
                keyframes.forEachIndexed { index, it ->
                    if (abs(it.time - time) < equalityDt) {
                        return keyframes[index].apply {
                            this.time = time
                            this.value = value
                        }
                    }
                }
            } else {
                keyframes.clear()
            }
            var index = keyframes.binarySearch { it.time.compareTo(time) }
            if (index < 0) index = -1 - index
            val interpolation = keyframes.getOrNull(index)?.interpolation ?: Interpolation.SPLINE
            val newFrame = Keyframe(time, value, interpolation)
            keyframes.add(newFrame)
            sort()
            return newFrame
        }
    }

    fun checkThread() {
        if (glThread != null && Thread.currentThread() != glThread &&
            !root.listOfAll.none {
                when (this) {
                    it.color, it.position, it.rotationYXZ,
                    it.colorMultiplier, it.skew -> true
                    else -> false
                }
            }
        ) {
            throw RuntimeException()
        }
    }

    /**
     * true, if found
     * */
    fun remove(keyframe: Keyframe<*>): Boolean {
        checkThread()
        synchronized(this) {
            return keyframes.remove(keyframe)
        }
    }

    operator fun get(t0: Double, t1: Double): List<Keyframe<V>> {
        val i0 = max(0, getIndexBefore(t0))
        val i1 = min(getIndexBefore(t1) + 1, keyframes.size)
        return if (i1 > i0) keyframes.subList(i0, i1).filter { it.time in t0..t1 }
        else emptyList()
    }

    fun getAnimatedValue(time: Double, dst: V? = null): V {
        synchronized(this) {
            val size = keyframes.size
            return when {
                size == 0 -> defaultValue
                size == 1 || !isAnimated -> keyframes[0].value
                else -> {

                    val index = clamp(getIndexBefore(time), 0, keyframes.size - 2)
                    val frame0 = keyframes.getOrElse(index - 1) { keyframes[0] }
                    val frame1 = keyframes[index]
                    val frame2 = keyframes[index + 1]
                    val frame3 = keyframes.getOrElse(index + 2) { keyframes.last() }
                    if (frame1 == frame2) return frame1.value

                    val t1 = frame1.time
                    val t2 = frame2.time

                    val f = (time - t1) / max(t2 - t1, 1e-16)
                    val w = getWeights(frame0, frame1, frame2, frame3, f)

                    // LOGGER.info("weights: ${w.print()}, values: ${frame0.value}, ${frame1.value}, ${frame2.value}, ${frame3.value}")

                    var valueSum: Any? = null
                    var weightSum = 0.0
                    fun addMaybe(value: V, weight: Double) {
                        if (weightSum == 0.0) {
                            valueSum = toCalc(value)
                            if (weight != 1.0) valueSum = mul(valueSum!!, weight, dst)
                            weightSum = weight
                        } else if (weight != 0.0) {
                            // add value with weight...
                            valueSum = mulAdd(valueSum!!, toCalc(value), weight, dst)
                            weightSum += weight
                        }// else done
                    }

                    addMaybe(frame0.value, w.x)
                    addMaybe(frame1.value, w.y)
                    addMaybe(frame2.value, w.z)
                    addMaybe(frame3.value, w.w)

                    return clamp(fromCalc(valueSum!!))

                }
            }
        }
    }

    operator fun get(time: Double, dst: V? = null) = getValueAt(time, dst)
    // todo access with dst types

    fun getValueAt(time: Double, dst: Any? = null): V {
        val hasDrivers = drivers.any { it != null }
        val animatedValue = getAnimatedValue(time)
        if (!hasDrivers) return animatedValue
        val v = animatedValue ?: defaultValue ?: 0.0
        val v0 = getDouble(v, 0)
        val v1 = getDouble(v, 1)
        val v2 = getDouble(v, 2)
        val v3 = getDouble(v, 3)
        // replace the components, which have drivers, with the driver values
        return when (animatedValue) {
            is Int -> drivers[0]?.getValue(time, v0)?.toInt() ?: animatedValue
            is Long -> drivers[0]?.getValue(time, v0)?.toLong() ?: animatedValue
            is Float -> drivers[0]?.getValue(time, v0)?.toFloat() ?: animatedValue
            is Double -> drivers[0]?.getValue(time, v0) ?: animatedValue
            is Vector2f -> AnimationMaths.v2(dst).set(
                drivers[0]?.getFloatValue(time, v0) ?: animatedValue.x,
                drivers[1]?.getFloatValue(time, v1) ?: animatedValue.y
            )
            is Vector3f -> AnimationMaths.v3(dst).set(
                drivers[0]?.getFloatValue(time, v0) ?: animatedValue.x,
                drivers[1]?.getFloatValue(time, v1) ?: animatedValue.y,
                drivers[2]?.getFloatValue(time, v2) ?: animatedValue.z
            )
            is Vector4f -> AnimationMaths.v4(dst).set(
                drivers[0]?.getFloatValue(time, v0) ?: animatedValue.x,
                drivers[1]?.getFloatValue(time, v1) ?: animatedValue.y,
                drivers[2]?.getFloatValue(time, v2) ?: animatedValue.z,
                drivers[3]?.getFloatValue(time, v3) ?: animatedValue.w
            )
            is Quaternionf -> AnimationMaths.q4(dst).set(
                drivers[0]?.getFloatValue(time, v0) ?: animatedValue.x,
                drivers[1]?.getFloatValue(time, v1) ?: animatedValue.y,
                drivers[2]?.getFloatValue(time, v2) ?: animatedValue.z,
                drivers[3]?.getFloatValue(time, v3) ?: animatedValue.w
            )
            else -> throw RuntimeException("Replacing components with drivers in $animatedValue is not yet supported!")
        } as V
    }

    private fun toCalc(a: V): Any {
        return when (a) {
            is Int -> a.toDouble() as Any
            is Float -> a
            is Double -> a
            is Long -> a.toDouble() as Any
            is Vector2fc, is Vector3fc, is Vector4fc, is Quaternionf -> a
            is Vector2dc, is Vector3dc, is Vector4dc, is Quaterniond -> a
            is String -> a
            else -> throw RuntimeException("don't know how to calc $a")
        } as Any // needed by Intellij Kotlin compiler
    }

    private fun fromCalc(a: Any): V = clampAny(a)

    private fun getIndexBefore(time: Double): Int {
        // get the index of the time
        val rawIndex = keyframes.binarySearch { it.time.compareTo(time) }
        return (if (rawIndex < 0) -rawIndex - 1 else rawIndex) - 1
    }

    override fun getClassName(): String = "AnimatedProperty"
    override fun getApproxSize(): Int = 10

    override fun save(writer: BaseWriter) {
        super.save(writer)
        sort()
        // must be written before keyframes!!
        if (isAnimated) {
            writer.writeBoolean("isAnimated", isAnimated)
            writer.writeObjectList(this, "vs", keyframes)
        } else {
            // isAnimated = false is default
            val value0 = keyframes.firstOrNull()?.value
            if (value0 != null && value0 != defaultValue) {
                writer.writeValue(this, "v", value0)
            }
        }
        for (i in 0 until min(type.components, drivers.size)) {
            writer.writeObject(this, "driver$i", drivers[i])
        }
    }

    fun sort() {
        synchronized(this) {
            keyframes.sort()
        }
    }

    override fun readSomething(name: String, value: Any?) {
        when (name) {
            "keyframe0", "v" -> addKeyframe(0.0, value ?: return)
            else -> super.readSomething(name, value)
        }
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "isAnimated" -> isAnimated = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "keyframes", "vs" -> {
                values.filterIsInstance<Keyframe<*>>().forEach { value ->
                    val castValue = type.acceptOrNull(value.value!!)
                    if (castValue != null) {
                        addKeyframe(value.time, clamp(castValue as V) as Any, 0.0)?.apply {
                            interpolation = value.interpolation
                        }
                    } else LOGGER.warn("Dropped keyframe!, incompatible type ${value.value} for $type")
                }
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "keyframes", "vs" -> {
                if (value is Keyframe<*>) {
                    val castValue = type.acceptOrNull(value.value!!)
                    if (castValue != null) {
                        addKeyframe(value.time, clamp(castValue as V) as Any, 0.0)?.apply {
                            interpolation = value.interpolation
                        }
                    } else LOGGER.warn("Dropped keyframe!, incompatible type ${value.value} for $type")
                } else WrongClassType.warn("keyframe", value)
            }
            "driver0" -> setDriver(0, value)
            "driver1" -> setDriver(1, value)
            "driver2" -> setDriver(2, value)
            "driver3" -> setDriver(3, value)
            else -> super.readObject(name, value)
        }
    }

    fun setDriver(index: Int, value: ISaveable?) {
        if (index >= drivers.size) {
            LOGGER.warn("Driver$index out of bounds for ${type.components}/${drivers.size}/$type")
            return
        }
        if (value is AnimationDriver) {
            drivers[index] = value
        } else WrongClassType.warn("driver", value)
    }

    fun copyFrom(obj: Any?, force: Boolean = false) {
        if (obj === this && !force) throw RuntimeException("Probably a typo!")
        if (obj is AnimatedProperty<*>) {
            isAnimated = obj.isAnimated
            keyframes.clear()
            obj.keyframes.forEach { src ->
                val castValue = type.acceptOrNull(src.value!!)
                if (castValue != null) {
                    val dst = Keyframe(src.time, clamp(castValue as V), src.interpolation)
                    keyframes.add(dst)
                } else LOGGER.warn("${src.value} is not accepted by $type")
                // else convert the type??...
            }
            for (i in 0 until type.components) {
                this.drivers[i] = obj.drivers.getOrNull(i)
            }
        } else LOGGER.warn("copy-from-object $obj is not an AnimatedProperty!")
    }

    fun clear() {
        isAnimated = false
        for (i in drivers.indices) {
            drivers[i] = null
        }
        keyframes.clear()
    }

    override fun isDefaultValue() =
        !isAnimated && (keyframes.isEmpty() || keyframes[0].value == defaultValue) && drivers.all { it == null }

}