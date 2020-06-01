package me.anno.objects.animation

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.lang.RuntimeException

class Keyframe<V>(var time: Float, var value: V): Saveable(), Comparable<Keyframe<V>> {
    override fun compareTo(other: Keyframe<V>): Int = time.compareTo(other.time)

    override fun getClassName(): String = "Keyframe"
    override fun getApproxSize(): Int = 1

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("time", time)
        writer.writeValue("value", value)
    }

    override fun readFloat(name: String, value: Float) {
        when(name){
            "time" -> time = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        when(name){
            "value" -> this.value = value as V
            else -> super.readSomething(name, value)
        }
    }

    fun setValue(index: Int, v: Float){
        value = when(val value = value){
            is Float -> v as Any
            is Vector2f -> when(index){
                0 -> Vector2f(v, value.y)
                else -> Vector2f(value.x, v)
            }
            is Vector3f -> when(index){
                0 -> Vector3f(v, value.y, value.z)
                1 -> Vector3f(value.x, v, value.z)
                else -> Vector3f(value.x, value.y, v)
            }
            is Vector4f -> when(index){
                0 -> Vector4f(v, value.y, value.z, value.w)
                1 -> Vector4f(value.x, v, value.z, value.w)
                2 -> Vector4f(value.x, value.y, v, value.w)
                else -> Vector4f(value.x, value.y, value.z, v)
            }
            is Quaternionf -> when(index){
                0 -> Quaternionf(v, value.y, value.z, value.w)
                1 -> Quaternionf(value.x, v, value.z, value.w)
                2 -> Quaternionf(value.x, value.y, v, value.w)
                else -> Quaternionf(value.x, value.y, value.z, v)
            }
            else -> throw RuntimeException("todo implement Keyframe.getValue(index) for $value")
        } as V
    }

    fun getValue(index: Int): Float {
        return when(val value = value){
            is Float -> value
            is Vector2f -> when(index){
                0 -> value.x
                else -> value.y
            }
            is Vector3f -> when(index){
                0 -> value.x
                1 -> value.y
                else -> value.z
            }
            is Vector4f -> when(index){
                0 -> value.x
                1 -> value.y
                2 -> value.z
                else -> value.w
            }
            is Quaternionf -> when(index){
                0 -> value.x
                1 -> value.y
                2 -> value.z
                else -> value.w
            }
            else -> throw RuntimeException("todo implement Keyframe.getValue(index) for $value")
        }
    }

    override fun isDefaultValue() = false

    companion object {
        fun BaseWriter.writeValue(name: String, v: Any?){
            when(v){
                is Float -> writeFloat(name, v)
                is Double -> writeDouble(name, v)
                is Vector3f -> writeVector3(name, v)
                is Vector4f -> writeVector4(name, v)
                else -> throw RuntimeException("todo implement")
            }
        }
    }

}