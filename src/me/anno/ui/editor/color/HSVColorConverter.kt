package me.anno.ui.editor.color

import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object HSVColorConverter {

    fun rgbToHSV(r: Float, g: Float, b: Float, dst: Vector3f = Vector3f()): Vector3f {
        val value = max(r, max(g, b))
        val min = min(r, min(g, b))
        val delta = value - min
        var hue = when {
            delta == 0f -> 0f
            value == r -> ((g-b)/delta)
            value == g -> (((b-r)/delta + 2))
            else -> (((r-g)/delta + 4))
        }
        if(hue < 0f) hue += 6f
        hue /= 6f
        val saturation = if(value == 0f) 0f else delta/value
        return dst.set(hue, saturation, value)
    }

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