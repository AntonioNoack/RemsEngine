package me.anno.utils

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.lang.RuntimeException
import java.nio.FloatBuffer
import kotlin.math.PI

object Floats {

    fun Any.anyToDouble(): Double {
        return when(this){
            is Int -> this.toDouble()
            is Long -> this.toDouble()
            is Float -> this.toDouble()
            is Double -> this
            else -> throw RuntimeException()
        }
    }

    fun Any.anyToFloat(): Float {
        return when(this){
            is Int -> this.toFloat()
            is Long -> this.toFloat()
            is Float -> this
            is Double -> this.toFloat()
            else -> throw RuntimeException()
        }
    }

    fun FloatBuffer.put3(v: Vector2f){
        put(v.x)
        put(v.y)
    }

    fun FloatBuffer.put3(v: Vector3f){
        put(v.x)
        put(v.y)
        put(v.z)
    }

    fun FloatBuffer.put3(v: Vector4f){
        put(v.x)
        put(v.y)
        put(v.z)
        put(v.w)
    }

    private val piF180 = (PI/180).toFloat()
    private val x180fPi = (180/PI).toFloat()

    fun Float.toDegrees() = this * x180fPi
    fun Float.toRadians() = this * piF180


}