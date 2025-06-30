package me.anno.ecs.components.mesh.utils

import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import org.joml.AABBf
import org.joml.Vector3f

class NormalHelperTree(initialCapacity: Int, bounds: AABBf, minVertexDistance: Float) :
    HashVertexLookup<FloatArrayList>(initialCapacity, bounds, minVertexDistance) {

    fun put(position: Vector3f, normal: Vector3f) {
        val gridIndex = gridIndex(position)
        val entry = entries.getOrPut(gridIndex) { FloatArrayList(15) }
        if (entry.size < 32 * 3) entry.add(normal) // else limit reached
    }
}
