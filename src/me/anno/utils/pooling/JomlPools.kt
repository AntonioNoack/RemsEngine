package me.anno.utils.pooling

import org.joml.*

object JomlPools {

    @JvmField
    val vec2i = Stack { Vector2i() }
    @JvmField
    val vec2f = Stack { Vector2f() }
    @JvmField
    val vec2d = Stack { Vector2d() }

    @JvmField
    val vec3i = Stack { Vector3i() }
    @JvmField
    val vec3f = Stack { Vector3f() }
    @JvmField
    val vec3d = Stack { Vector3d() }

    @JvmField
    val vec4i = Stack { Vector4i() }
    @JvmField
    val vec4f = Stack { Vector4f() }
    @JvmField
    val vec4d = Stack { Vector4d() }

    @JvmField
    val quat4f = Stack { Quaternionf() }
    @JvmField
    val quat4d = Stack { Quaterniond() }

    @JvmField
    val mat2f = Stack { Matrix2f() }
    @JvmField
    val mat2d = Stack { Matrix2d() }

    @JvmField
    val mat3f = Stack { Matrix3f() }
    @JvmField
    val mat3d = Stack { Matrix3d() }

    @JvmField
    val mat4f = Stack { Matrix4f() }
    @JvmField
    val mat4d = Stack { Matrix4d() }

    @JvmField
    val mat4x3f = Stack { Matrix4x3f() }
    @JvmField
    val mat4x3d = Stack { Matrix4x3d() }

    @JvmField
    val aabbf = Stack { AABBf() }
    @JvmField
    val aabbd = Stack { AABBd() }

    @JvmStatic
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