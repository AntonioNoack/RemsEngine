package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachLineIndex
import me.anno.maths.Packing.pack64
import me.anno.utils.structures.arrays.BooleanArrayList
import kotlin.math.max
import kotlin.math.min

object OnEdgeCalculator {

    /**
     * Calculate which vertices are on the edge of a Mesh.
     * Aka which triangles are on the edge.
     * */
    fun calculateIsOnEdge(mesh: Mesh, dst: BooleanArrayList): BooleanArrayList {

        dst.fill(true)

        // if there is few vertices, we could use an IntArray for counting
        val sides = HashMap<Long, Int>()
        var index = 0
        mesh.forEachLineIndex { ai, bi ->
            val min = min(ai, bi)
            val max = max(ai, bi)
            val hash = pack64(min, max)
            val previousIndex = sides.put(hash, index)
            if (previousIndex != null) {
                // it was already included -> this side is an edge
                dst[previousIndex] = false
                dst[index] = false
            }
            index++
            false
        }

        return dst
    }
}