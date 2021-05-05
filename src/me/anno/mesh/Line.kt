package me.anno.mesh

import org.joml.Vector3f
import org.joml.Vector3fc

data class Line(val a: Vector3f, val b: Vector3f){
    fun scale(s: Float){
        a.mul(s)
        b.mul(s)
    }
    fun switchYZ(){
        a.set(a.x, a.z, -a.y)
        b.set(b.x, b.z, -b.y)
    }
    fun switchXZ(){
        a.set(a.z, a.y, -a.x)
        b.set(b.z, b.y, -b.x)
    }
    fun translate(delta: Vector3fc){
        a.add(delta)
        b.add(delta)
    }
}