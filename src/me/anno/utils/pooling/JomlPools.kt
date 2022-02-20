package me.anno.utils.pooling

import org.joml.*

object JomlPools {

    val vec2i = Stack { Vector2i() }
    val vec2f = Stack { Vector2f() }
    val vec2d = Stack { Vector2d() }

    val vec3i = Stack { Vector3i() }
    val vec3f = Stack { Vector3f() }
    val vec3d = Stack { Vector3d() }

    val vec4i = Stack { Vector4i() }
    val vec4f = Stack { Vector4f() }
    val vec4d = Stack { Vector4d() }

    val quat4f = Stack { Quaternionf() }
    val quat4d = Stack { Quaterniond() }

    val mat2f = Stack { Matrix2f() }
    val mat2d = Stack { Matrix2d() }

    val mat3f = Stack { Matrix3f() }
    val mat3d = Stack { Matrix3d() }

    val mat4f = Stack { Matrix4f() }
    val mat4d = Stack { Matrix4d() }

    val mat4x3f = Stack { Matrix4x3f() }
    val mat4x3d = Stack { Matrix4x3d() }

    val aabbf = Stack { AABBf() }
    val aabbd = Stack { AABBd() }

    fun reset() {

        vec2i.reset()
        vec2f.reset()
        vec2d.reset()

        vec3i.reset()
        vec3f.reset()
        vec3d.reset()

        vec4i.reset()
        vec4f.reset()
        vec4d.reset()

        quat4f.reset()
        quat4d.reset()

        mat2f.reset()
        mat2d.reset()

        mat3f.reset()
        mat3d.reset()

        mat4f.reset()
        mat4d.reset()

        mat4x3f.reset()
        mat4x3d.reset()

        aabbf.reset()
        aabbd.reset()

    }

}