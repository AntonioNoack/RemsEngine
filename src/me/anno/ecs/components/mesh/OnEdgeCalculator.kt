package me.anno.ecs.components.mesh

import kotlin.math.max
import kotlin.math.min

object OnEdgeCalculator {

    fun calculateIsOnEdge(mesh: Mesh, dst: BooleanArray = BooleanArray(mesh.numTriangles * 3)): BooleanArray {

        dst.fill(true)

        // calculate isOnEdge
        // if there is few vertices, we could use an IntArray for counting
        val sides = HashMap<Long, Int>()
        var index = 0
        mesh.forEachSideIndex { a, b ->
            val min = min(a, b).toLong()
            val max = max(a, b).toLong()
            val hash = min.shl(32) or max
            val previousIndex = sides.put(hash, index)
            if (previousIndex != null) {
                // it was already included -> this side a an edge
                dst[previousIndex] = false
                dst[index] = false
            }
            index++
        }

        return dst

    }

}