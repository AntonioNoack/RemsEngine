package me.anno.utils.pooling

import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i

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
}