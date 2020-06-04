package me.anno.ui.editor.color

import org.joml.Vector3f
import kotlin.math.abs

object HSVColorConverter {

    fun hsvToRGB(h: Float, s: Float, v: Float, dst: Vector3f = Vector3f()): Vector3f {
        val c = v * s
        val x = c * (1 - abs((h*6) % 2f - 1f))
        val m = v - c
        when((h*6).toInt()){
            0 -> dst.set(c,x,0f)
            1 -> dst.set(x,c,0f)
            2 -> dst.set(0f,c,x)
            3 -> dst.set(0f,x,c)
            4 -> dst.set(x,0f,c)
            else -> dst.set(c,0f,x)
        }
        dst.add(m, m, m)
        return dst
    }

}