package me.anno.mesh

import org.joml.Vector2f
import org.joml.Vector3f

class Point(val position: Vector3f, val normal: Vector3f, val uv: Vector2f?){
    fun flipV(){
        if(uv != null){
            uv.y = 1f - uv.y
        }
    }

    operator fun component1() = position
    operator fun component2() = normal
    operator fun component3() = uv

}