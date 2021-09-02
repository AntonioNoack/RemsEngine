package me.anno.utils.pooling

import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

object JomlPools {

    val vector3f = Stack { Vector3f() }
    val vector3d = Stack { Vector3d() }
    val quat4f = Stack { Quaternionf() }
    val quat4d = Stack { Quaterniond() }

}