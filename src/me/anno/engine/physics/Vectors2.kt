package me.anno.engine.physics

import org.joml.Vector3d

object Vectors2 {

    fun Vector3d.set2(v: javax.vecmath.Vector3d): Vector3d {
        return this.set(v.x, v.y, v.z)
    }

}