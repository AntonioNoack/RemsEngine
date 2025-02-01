package me.anno.maths.bvh

import me.anno.maths.bvh.TLASTexture.putBounds
import org.joml.AABBf
import java.nio.FloatBuffer

fun interface BLASFiller {

    fun fill(v0: Int, v1: Int, bounds: AABBf)

    companion object {

        fun fillBLAS(
            roots: List<BLASNode>,
            multiplier: Int,
            data: FloatBuffer
        ) = fillBLAS(roots, multiplier) { v0, v1, bounds ->
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
            roots: List<BLASNode>,
            multiplier: Int,
            callback: BLASFiller
        ) {

            if (multiplier < 3) {
                throw IllegalArgumentException("Cannot represent x,y,z this way.")
            }

            var nextId = 0
            for (index in roots.indices) {
                val bvh = roots[index]
                bvh.forEach {
                    it.nodeId = nextId++
                }
            }

            // assign indices to all nodes
            for (index in roots.indices) {
                val blasRoot = roots[index]
                blasRoot.forEach { node ->

                    val v0: Int
                    val v1: Int

                    if (node is BLASBranch) {
                        v0 = node.n1.nodeId - node.nodeId // next node
                        v1 = node.axis // not a leaf, 0-2
                    } else {
                        node as BLASLeaf
                        v0 = (node.start + node.triangleStartIndex) * multiplier
                        // >= 3, < 3 would mean not a single triangle, and that's invalid
                        v1 = node.length * multiplier
                    }

                    val bounds = node.bounds
                    callback.fill(v0, v1, bounds)
                }
            }
        }
    }
}