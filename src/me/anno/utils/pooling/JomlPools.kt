package me.anno.utils.pooling

import org.joml.Vector3d
import org.joml.Vector3f

object JomlPools {

    val vector3f = Stack { Vector3f() }
    val vector3d = Stack { Vector3d() }

}