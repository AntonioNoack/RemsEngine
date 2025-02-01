package me.anno.maths.bvh

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
            fillBLASNode(data, v0, v1, bounds)
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
                val bvh = roots[index]
                bvh.forEach {

                    val v0: Int
                    val v1: Int

                    if (it is BLASBranch) {
                        v0 = it.n1.nodeId - it.nodeId // next node
                        v1 = it.axis // not a leaf, 0-2
                    } else {
                        it as BLASLeaf
                        v0 = (it.start + it.triangleStartIndex) * multiplier
                        // >= 3, < 3 would mean not a single triangle, and that's invalid
                        v1 = it.length * multiplier
                    }

                    val bounds = it.bounds
                    callback.fill(v0, v1, bounds)

                }
            }
        }

        private fun fillBLASNode(data: FloatBuffer, v0: Int, v1: Int, bounds: AABBf) {

            // root node
            // aabb = 6x fp32
            // child0 can directly follow
            // child1 needs offset; 1x int32
            // leaf node
            // aabb = 6x fp32
            // start, length = 2x int32
            // for both types just use 8x4 = 32 bytes
            // we will find a place for markers about the type :)

            data.put(bounds.minX)
            data.put(bounds.minY)
            data.put(bounds.minZ)
            data.put(Float.fromBits(v0))

            data.put(bounds.maxX)
            data.put(bounds.maxY)
            data.put(bounds.maxZ)
            data.put(Float.fromBits(v1))
        }
    }
}