package me.anno.objects.animation

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
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