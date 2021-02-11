package me.anno.objects.modes

import me.anno.objects.Transform
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.abs

class RotateJPEG(val mirrorHorizontal: Boolean, val mirrorVertical: Boolean, val angleCW: Int){
    val switchWH = (abs(angleCW) % 180) > 45
    private val angleRadians = -(Math.PI * angleCW / 180).toFloat() // CCW
    fun apply(stack: Matrix4f){
        if(mirrorHorizontal){
            stack.scale(Vector3f(1f, -1f, 1f))
        } else if(mirrorVertical){
            stack.scale(Vector3f(-1f, 1f, 1f))
        }
        if(angleCW != 0){
            stack.rotate(angleRadians, Transform.zAxis)
        }
    }
    fun isNull() = !mirrorHorizontal && !mirrorVertical && angleCW == 0
}