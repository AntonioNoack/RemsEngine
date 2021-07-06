package me.anno.animation

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

open class TimeValue<V>(var time: Double, var value: V) : Saveable() {

    override val className get() = "TimeValue"
    override val approxSize = 1
    override fun isDefaultValue() = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("time", time)
        writer.writeSomething(this, "value", value, true)
    }

    override fun readDouble(name: String, value: Double) {
        when (name) {
            "time" -> time = value
            else -> super.readDouble(name, value)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        when (name) {
            "value" -> this.value = value as V
            else -> super.readSomething(name, value)
        }
    }

    fun setValue(index: Int, v: Float, type: Type) {
        value = type.clamp(
            when (val value = value) {
                is Int -> v.toInt()
                is Long -> v.toLong()
                is Float -> v
                is Double -> v.toDouble()
                is Vector2f -> when (index) {
                    0 -> Vector2f(v, value.y)
                    else -> Vector2f(value.x, v)
                }
                is Vector3f -> when (index) {
                    0 -> Vector3f(v, value.y, value.z)
                    1 -> Vector3f(value.x, v, value.z)
                    else -> Vector3f(value.x, value.y, v)
                }
                is Vector4f -> when (index) {
                    0 -> Vector4f(v, value.y, value.z, value.w)
                    1 -> Vector4f(value.x, v, value.z, value.w)
                    2 -> Vector4f(value.x, value.y, v, value.w)
                    else -> Vector4f(value.x, value.y, value.z, v)
                }
                is Quaternionf -> when (index) {
                    0 -> Quaternionf(v, value.y, value.z, value.w)
                    1 -> Quaternionf(value.x, v, value.z, value.w)
                    2 -> Quaternionf(value.x, value.y, v, value.w)
                    else -> Quaternionf(value.x, value.y, value.z, v)
                }
                is String -> v
                else -> throw RuntimeException("todo implement Keyframe.getValue(index) for $value")
            } as V
        )
    }

}
