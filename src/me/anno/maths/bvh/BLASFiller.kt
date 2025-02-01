package me.anno.maths.bvh

import me.anno.maths.bvh.TLASTexture.putBounds
import org.joml.AABBf
import java.nio.FloatBuffer

fun interface BLASFiller {

    fun fill(v0: Int, v1: Int, bounds: AABBf)

    companion object {

        fun fillBLAS(
            roots: List<BLASNode>,
            triangleIndexMultiplier: Int,
            data: FloatBuffer
        ) = fillBLAS(roots, triangleIndexMultiplier) { v0, v1, bounds ->
            // root node
            // aabb = 6x fp32
            // child0 can directly follow
            // child1 needs offset; 1x int32
            // leaf node
            // aabb = 6x fp32
            // start, length = 2x int32
            // for both types just use 8x4 = 32 bytes
            // we will find a place for markers about the type :)
            putBounds(data, bounds, v0, v1)
        }

        fun fillBLAS(
            blasRoots: List<BLASNode>,
            triangleIndexMultiplier: Int,
            callback: BLASFiller
        ) {

            if (triangleIndexMultiplier < 3) {
                throw IllegalArgumentException("Cannot represent x,y,z this way.")
            }

            var nextId = 0
            for (index in blasRoots.indices) {
                val blasRoot = blasRoots[index]
                blasRoot.forEach { blasNode ->
                    blasNode.nodeId = nextId++
                }
            }

            // assign indices to all nodes
            for (index in blasRoots.indices) {
                val blasRoot = blasRoots[index]
                blasRoot.forEach { node ->

                    val v0: Int
                    val v1: Int

                    if (node is BLASBranch) {
                        v0 = node.n1.nodeId - node.nodeId // next node
                        v1 = node.axis // not a leaf, 0-2
                    } else {
                        node as BLASLeaf
                        v0 = (node.start + node.triangleStartIndex) * triangleIndexMultiplier
                        // >= 3, < 3 would mean not a single triangle, and that's invalid
                        v1 = node.length * triangleIndexMultiplier
                    }

                    val bounds = node.bounds
                    callback.fill(v0, v1, bounds)
                }
            }
        }
    }
}