package me.anno.objects.meshes

import me.anno.objects.cache.CacheData
import org.joml.Matrix4fArrayList
import org.joml.Vector4f

abstract class MeshData: CacheData {

    abstract fun draw(stack: Matrix4fArrayList, time: Double, color: Vector4f)

}