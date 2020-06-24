package me.anno.objects.animation

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.drivers.AnimationDriver
import me.anno.utils.WrongClassType
import org.joml.*
import java.lang.RuntimeException
import kotlin.math.abs

class AnimatedProperty<V>(val type: Type, val minValue: V?, val maxValue: V?): Saveable(){

    constructor(type: Type): this(type, null, null)

    enum class Type(
        val code: String,
        val defaultValue: Any,
        val components: Int,
        val unitScale: Float,
        val hasLinear: Boolean,
        val hasExponential: Boolean,
        val accepts: (Any?) -> Boolean){
        FLOAT("float", 0f, 1, 1f, true, true, { it is Float }),
        FLOAT_01("float01", 0f, 1, 1f, true, true, { it is Float }),
        FLOAT_PLUS("float+", 0f, 1, 1f, false, true, { it is Float }),
        VEC2("vec2", Vector2f(), 2, 1f, true, true, { it is Vector2f }),
        VEC3("vec3", Vector3f(), 3, 1f, true, true, { it is Vector3f }),
        POSITION("pos", Vector3f(), 3, 1f, true, true, { it is Vector3f }),
        SCALE("scale", Vector3f(1f, 1f, 1f), 3, 1f, true, true, { it is Vector3f }),
        ROT_YXZ("rotYXZ", Vector3f(), 3, 360f, true, true, { it is Vector3f }),
        SKEW_2D("skew2D", Vector2f(), 2, 1f, true, true, { it is Vector2f }),
        QUATERNION("quaternion", Quaternionf(), 4, 1f, true, true, { it is Quaternionf }),
        COLOR("color", Vector4f(1f,1f,1f,1f), 4, 1f, true, true, { it is Vector4f }),
        TILING("tiling", Vector4f(1f, 1f, 0f, 0f), 4, 1f, true, true, { it is Vector4f });
        init { types[code] = this }
    }

    companion object {
        val types = HashMap<String, Type>()
        fun float() = AnimatedProperty<Float>(Type.FLOAT)
        fun vec2() = AnimatedProperty<Vector2f>(Type.VEC2)
        fun vec3() = AnimatedProperty<Vector3f>(Type.VEC3)
        fun floatPlus() = AnimatedProperty(Type.FLOAT_PLUS, 0f, null)
        fun float01() = AnimatedProperty(Type.FLOAT_01, 0f, 1f)
        fun pos() = AnimatedProperty<Vector3f>(Type.POSITION)
        fun rotYXZ() = AnimatedProperty<Vector3f>(Type.ROT_YXZ)
        fun scale() = AnimatedProperty<Vector3f>(Type.SCALE)
        fun color() = AnimatedProperty<Vector4f>(Type.COLOR)
        fun quat() = AnimatedProperty<Quaternionf>(Type.QUATERNION)
        fun skew() = AnimatedProperty<Vector2f>(Type.SKEW_2D)
        fun tiling() = AnimatedProperty<Vector4f>(Type.TILING)
    }

    val drivers
            = arrayOfNulls<AnimationDriver>(type.components)

    var isAnimated = false
    val keyframes = ArrayList<Keyframe<V>>()
    var interpolation = Interpolation.LINEAR_BOUNDED

    fun ensureCorrectType(v: Any?): V {
        if(!type.accepts(v)) throw RuntimeException("got $v for $type")
        return v as V
    }

    fun clamp(value: V): V {
        if(minValue != null || maxValue != null){
            value as Comparable<V>
            if(minValue != null && value < minValue) return minValue
            if(maxValue != null && value >= maxValue) return maxValue
        }
        return value
    }

    fun set(value: V): AnimatedProperty<V> {
        keyframes.clear()
        return add(0f, value)
    }

    fun add(time: Float, value: V): AnimatedProperty<V> {
        keyframes.add(Keyframe(time, clamp(value)))
        keyframes.sort()
        return this
    }

    fun addKeyframe(time: Float, value: Any) =
        addKeyframe(time, value, 0.001f)

    fun addKeyframe(time: Float, value: Any, equalityDt: Float){
        if(type.accepts(value)){
            addKeyframeInternal(time, clamp(value as V), equalityDt)
        }
    }

    private fun addKeyframeInternal(time: Float, value: V, equalityDt: Float){
        ensureCorrectType(value)
        keyframes.forEachIndexed { index, it ->
            if(abs(it.time - time) < equalityDt){
                keyframes[index] = Keyframe(time, value)
                return
            }
        }
        keyframes.add(Keyframe(time, value))
        sort()
    }

    fun remove(keyframe: Keyframe<*>){
        keyframes.remove(keyframe)
    }

    operator fun get(time: Float) = getValueAt(time)

    fun getAnimatedValue(time: Float): V {
        return when(keyframes.size){
            0 -> type.defaultValue as V
            1 -> keyframes[0].value
            else -> {
                val index = getIndexBefore(time)
                val frame0 = keyframes.getOrElse(index){ keyframes[0] }
                val frame1 = keyframes.getOrElse(index+1){ keyframes.last() }
                if(frame0 == frame1) return frame0.value
                val t0 = frame0.time
                val t1 = frame1.time
                when(interpolation){
                    Interpolation.STEP -> {
                        (if(time < t1) frame0 else frame1).value
                    }
                    Interpolation.LINEAR_UNBOUNDED -> {
                        val relativeTime = (time-t0)/(t1-t0)
                        lerp(frame0.value, frame1.value, relativeTime)
                    }
                    Interpolation.LINEAR_BOUNDED -> {
                        if(time <= t0) return frame0.value
                        if(time >= t1) return frame1.value
                        val relativeTime = (time-t0)/(t1-t0)
                        lerp(frame0.value, frame1.value, relativeTime)
                    }
                    else -> throw RuntimeException("todo interpolation $interpolation")
                }
            }
        }
    }

    fun getValueAt(time: Float): V {
        val animatedValue = if(drivers.all { it != null })
            type.defaultValue
        else getAnimatedValue(time)
        return if(drivers.all { it == null}) animatedValue
        else {
            // replace the components, which have drivers, with the driver values
            when(animatedValue){
                is Float -> drivers[0]?.getValue(time) ?: animatedValue
                is Vector2f -> Vector2f(
                    drivers[0]?.getValue(time) ?: animatedValue.x,
                    drivers[1]?.getValue(time) ?: animatedValue.y
                )
                is Vector3f -> Vector3f(
                    drivers[0]?.getValue(time) ?: animatedValue.x,
                    drivers[1]?.getValue(time) ?: animatedValue.y,
                    drivers[2]?.getValue(time) ?: animatedValue.z
                )
                is Vector4f -> Vector4f(
                    drivers[0]?.getValue(time) ?: animatedValue.x,
                    drivers[1]?.getValue(time) ?: animatedValue.y,
                    drivers[2]?.getValue(time) ?: animatedValue.z,
                    drivers[3]?.getValue(time) ?: animatedValue.w
                )
                is Quaternionf -> Quaternionf(
                    drivers[0]?.getValue(time) ?: animatedValue.x,
                    drivers[1]?.getValue(time) ?: animatedValue.y,
                    drivers[2]?.getValue(time) ?: animatedValue.z,
                    drivers[3]?.getValue(time) ?: animatedValue.w
                )
                else -> throw RuntimeException("Replacing components with drivers in $animatedValue is not yet supported!")
            }
        } as V
    }

    fun lerp(a: Float, b: Float, f: Float, g: Float) = a*g+b*f

    fun lerp(a: V, b: V, f: Float): V {
        val g = 1f-f
        return when(type){
            Type.FLOAT,
            Type.FLOAT_01,
            Type.FLOAT_PLUS -> (a as Float)*g+f*(b as Float)
            // Type.DOUBLE -> (a as Double)*g+f*(b as Double)
            Type.SKEW_2D -> (a as Vector2f).lerp(b as Vector2f, f, Vector2f())
            Type.POSITION,
            Type.ROT_YXZ,
            Type.SCALE -> (a as Vector3f).lerp(b as Vector3f, f, Vector3f())
            Type.COLOR, Type.TILING -> (a as Vector4f).lerp(b as Vector4f, f, Vector4f())
            Type.QUATERNION -> (a as Quaternionf).slerp(b as Quaternionf, f)
            else -> throw RuntimeException("don't know how to lerp $a and $b")
        } as V
    }

    fun getIndexBefore(time: Float): Int {
        // get the index of the time
        val rawIndex = keyframes.binarySearch { it.time.compareTo(time) }
        return (if(rawIndex < 0) -rawIndex-1 else rawIndex) - 1
    }

    override fun getClassName(): String = "AnimatedProperty<${type.code}>"
    override fun getApproxSize(): Int = 10

    override fun save(writer: BaseWriter) {
        super.save(writer)
        sort()
        writer.writeList(this, "keyframes", keyframes)
        writer.writeBool("isAnimated", isAnimated, true)
        for(i in 0 until 4){
            writer.writeObject(this, "driver$i", drivers[i])
        }
    }

    fun sort(){
        keyframes.sort()
    }

    override fun readBool(name: String, value: Boolean) {
        when(name){
            "isAnimated" -> isAnimated = value
            else -> super.readBool(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when(name){
            "keyframes" -> {
                if(value is Keyframe<*>){
                    if(type.accepts(value.value)){
                        addKeyframe(value.time, clamp(value.value as V) as Any, 1e-5f) // do clamp?
                    } else println("Dropped keyframe!, incompatible type ${value.value} for $type")
                } else WrongClassType.warn("keyframe", value)
            }
            "driver0" -> setDriver(0, value)
            "driver1" -> setDriver(1, value)
            "driver2" -> setDriver(2, value)
            "driver3" -> setDriver(3, value)
            else -> super.readObject(name, value)
        }
    }

    fun setDriver(index: Int, value: ISaveable?){
        if(index >= drivers.size) return
        if(value is AnimationDriver){
            drivers[index] = value
        } else WrongClassType.warn("driver", value)
    }

    // todo this may result in an issue, where we can't copy this object 1:1...
    // todo do we want this anyways?
    fun copyFrom(obj: Any?, force: Boolean = false){
        if(obj === this && !force) throw RuntimeException("Probably a typo!")
        if(obj is AnimatedProperty<*> && obj.type == type){
            isAnimated = obj.isAnimated
            keyframes.clear()
            keyframes.addAll(obj.keyframes as List<Keyframe<V>>)
            interpolation = obj.interpolation
        }
    }

    override fun isDefaultValue() = keyframes.isEmpty()

}