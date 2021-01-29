package me.anno.utils.types

import org.joml.Matrix4d
import org.joml.Matrix4f

object Matrices {

    fun Matrix4f.skew(x: Float, y: Float){
        mul3x3(// works
            1f, y, 0f,
            x, 1f, 0f,
            0f, 0f, 1f
        )
    }

    fun Matrix4d.skew(x: Double, y: Double){
        mul3x3(// works
            1.0, y, 0.0,
            x, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
    }

}