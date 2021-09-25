package me.anno.utils.types

import org.joml.*
import java.lang.RuntimeException
import java.nio.FloatBuffer
import java.util.*
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

    fun FloatBuffer.put3(v: Vector2fc){
        put(v.x())
        put(v.y())
    }

    fun FloatBuffer.put3(v: Vector3fc){
        put(v.x())
        put(v.y())
        put(v.z())
    }

    fun FloatBuffer.put3(v: Vector4fc){
        put(v.x())
        put(v.y())
        put(v.z())
        put(v.w())
    }

    private val piF180 = (PI/180).toFloat()
    private val x180fPi = (180/PI).toFloat()

    fun Float.toDegrees() = this * x180fPi
    fun Float.toRadians() = this * piF180

    fun Float.f6() = "%.6f".format(Locale.ENGLISH,this)
    fun Float.f5() = "%.5f".format(Locale.ENGLISH,this)
    fun Float.f4() = "%.4f".format(Locale.ENGLISH,this)
    fun Float.f3() = "%.3f".format(Locale.ENGLISH,this)
    fun Float.f2() = "%.2f".format(Locale.ENGLISH,this)
    fun Float.f1() = "%.1f".format(Locale.ENGLISH,this)

    fun Double.f6() = "%.6f".format(Locale.ENGLISH,this)
    fun Double.f5() = "%.5f".format(Locale.ENGLISH,this)
    fun Double.f4() = "%.4f".format(Locale.ENGLISH,this)
    fun Double.f3() = "%.3f".format(Locale.ENGLISH,this)
    fun Double.f2() = "%.2f".format(Locale.ENGLISH,this)
    fun Double.f1() = "%.1f".format(Locale.ENGLISH,this)

    fun Float.f6s() = "% .6f".format(Locale.ENGLISH,this)
    fun Float.f5s() = "% .5f".format(Locale.ENGLISH,this)
    fun Float.f4s() = "% .4f".format(Locale.ENGLISH,this)
    fun Float.f3s() = "% .3f".format(Locale.ENGLISH,this)
    fun Float.f2s() = "% .2f".format(Locale.ENGLISH,this)
    fun Float.f1s() = "% .1f".format(Locale.ENGLISH,this)

    fun Double.f6s() = "% .6f".format(Locale.ENGLISH,this)
    fun Double.f5s() = "% .5f".format(Locale.ENGLISH,this)
    fun Double.f4s() = "% .4f".format(Locale.ENGLISH,this)
    fun Double.f3s() = "% .3f".format(Locale.ENGLISH,this)
    fun Double.f2s() = "% .2f".format(Locale.ENGLISH,this)
    fun Double.f1s() = "% .1f".format(Locale.ENGLISH,this)


}