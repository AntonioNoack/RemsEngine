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
    val vec2i = Stack(Vector2i::class)
    val vec2f = Stack(Vector2f::class)
    val vec2d = Stack(Vector2d::class)
    val vec3i = Stack(Vector3i::class)
    val vec3f = Stack(Vector3f::class)
    val vec3d = Stack(Vector3d::class)
    val vec4i = Stack(Vector4i::class)
    val vec4f = Stack(Vector4f::class)
    val vec4d = Stack(Vector4d::class)
    val quat4f = Stack(Quaternionf::class)
    val quat4d = Stack(Quaterniond::class)
    val mat2f = Stack(Matrix2f::class)
    val mat2d = Stack(Matrix2d::class)
    val mat3f = Stack(Matrix3f::class)
    val mat3d = Stack(Matrix3d::class)
    val mat4f = Stack(Matrix4f::class)
    val mat4d = Stack(Matrix4d::class)
    val mat4x3f = Stack(Matrix4x3f::class)
    val mat4x3d = Stack(Matrix4x3d::class)
    val aabbf = Stack(AABBf::class)
    val aabbd = Stack(AABBd::class)
}