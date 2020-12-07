package me.anno.mesh

import org.joml.Vector2f
import org.joml.Vector3f

class Point(val position: Vector3f, val normal: Vector3f, val uv: Vector2f?){

    fun flipV(){
        if(uv != null){
            uv.y = 1f - uv.y
        }
    }

    fun scale(scale: Float){
        position.mul(scale)
    }

    fun switchYZ(){
        position.set(position.x, position.z, -position.y)
        normal.set(normal.x, normal.z, -normal.y)
    }

    fun translate(delta: Vector3f){
        position.add(delta)
    }

    operator fun component1() = position
    operator fun component2() = normal
    operator fun component3() = uv

}