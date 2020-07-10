package me.anno.utils

import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.*

fun DoubleArray.toVec3() = Vector3f(this[0].toFloat(), this[1].toFloat(), this[2].toFloat())

operator fun Any.get(index: Int, defaultValue: Float = 0f): Float {
    return when(this){
        is Float -> when(index){
            0 -> this
            else -> defaultValue
        }
        is Vector2f -> when(index){
            0 -> x
            1 -> y
            else -> defaultValue
        }
        is Vector3f -> when(index){
            0 -> x
            1 -> y
            2 -> z
            else -> defaultValue
        }
        is Vector4f -> when(index){
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> defaultValue
        }
        is Quaternionf -> when(index){
            0 -> x
            1 -> y
            2 -> z
            3 -> w
            else -> defaultValue
        }
        else -> defaultValue
    }
}