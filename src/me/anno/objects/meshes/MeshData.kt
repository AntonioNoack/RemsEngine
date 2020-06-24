package me.anno.objects.meshes

import me.anno.objects.cache.CacheData
import org.joml.Matrix4fStack
import org.joml.Vector4f

abstract class MeshData: CacheData {

    abstract fun draw(stack: Matrix4fStack, time: Float, color: Vector4f)

}