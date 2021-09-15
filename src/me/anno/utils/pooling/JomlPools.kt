package me.anno.utils.pooling

import org.joml.*

object JomlPools {

    val vec3f = Stack { Vector3f() }
    val vec3d = Stack { Vector3d() }
    val quat4f = Stack { Quaternionf() }
    val quat4d = Stack { Quaterniond() }
    val mat4f = Stack { Matrix4f() }
    val mat4d = Stack { Matrix4d() }

}