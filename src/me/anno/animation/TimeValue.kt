package me.anno.animation

import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.objects.modes.EditorFPS
import me.anno.objects.modes.LoopingState
import me.anno.objects.modes.UVProjection
import org.joml.*

open class TimeValue<V>(var time: Double, var value: V) : Saveable() {

    override fun getClassName() = "TimeValue"
    override fun getApproxSize() = 1
    override fun isDefaultValue() = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeDouble("time", time)
        writer.writeValue(this, "value", value)
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

    companion object {
        fun BaseWriter.writeValue(self: ISaveable?, name: String, v: Any?) {
            when (v) {
                is Boolean -> writeBoolean(name, v, true)
                is Int -> writeInt(name, v, true)
                is Long -> writeLong(name, v, true)
                is Float -> writeFloat(name, v, true)
                is Double -> writeDouble(name, v, true)
                is Vector2f -> writeVector2f(name, v, true)
                is Vector3f -> writeVector3f(name, v, true)
                is Vector4f -> writeVector4f(name, v, true)
                is String -> writeString(name, v, true)
                is Vector4d -> writeVector4d(name, v, true)
                null -> Unit /* mmh ... */
                is Filtering -> writeInt(name, v.id, true)
                is Clamping -> writeInt(name, v.id, true)
                is EditorFPS -> writeInt(name, v.value, true)
                is LoopingState -> writeInt(name, v.id, true)
                is ISaveable -> writeObject(self, name, v, true)
                is UVProjection -> writeInt(name, v.id, true)
                else -> throw RuntimeException("todo implement writing $v")
            }
        }
    }

}
