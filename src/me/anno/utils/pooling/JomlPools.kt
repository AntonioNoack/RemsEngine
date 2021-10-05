package me.anno.utils.pooling

import org.joml.*

object JomlPools {

    val vec2f = Stack { Vector2f() }
    val vec2d = Stack { Vector2d() }

    val vec3f = Stack { Vector3f() }
    val vec3d = Stack { Vector3d() }

    val quat4f = Stack { Quaternionf() }
    val quat4d = Stack { Quaterniond() }

    val mat3f = Stack { Matrix4f() }
    val mat3d = Stack { Matrix4d() }

    val mat4f = Stack { Matrix4f() }
    val mat4d = Stack { Matrix4d() }

    val mat4x3f = Stack { Matrix4x3f() }
    val mat4x3d = Stack { Matrix4x3d() }

    fun reset() {

        vec2f.reset()
        vec2d.reset()

        vec3f.reset()
        vec3d.reset()

        quat4f.reset()
        quat4d.reset()

        mat3f.reset()
        mat3d.reset()

        mat4f.reset()
        mat4d.reset()

        mat4x3f.reset()
        mat4x3d.reset()

    }

}