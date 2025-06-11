package com.bulletphysics.extras.gimpact

/**
 * @author jezek2
 */
class BvhTreeNodeArray {
    private var size = 0

    private var bounds = DoubleArray(0)
    private var indices = IntArray(0)

    fun clear() {
        size = 0
    }

    fun resize(newSize: Int) {
        val newBound = DoubleArray(newSize * 6)
        val newIndices = IntArray(newSize)

        System.arraycopy(bounds, 0, newBound, 0, size * 6)
        System.arraycopy(indices, 0, newIndices, 0, size)

        bounds = newBound
        indices = newIndices

        size = newSize
    }

    fun set(dstIdx: Int, array: BvhTreeNodeArray, srcIdx: Int) {
        val dstPos = dstIdx * 6
        val srcPos = srcIdx * 6

        val aBound = array.bounds
        val bounds = this.bounds
        bounds[dstPos] = aBound[srcPos]
        bounds[dstPos + 1] = aBound[srcPos + 1]
        bounds[dstPos + 2] = aBound[srcPos + 2]
        bounds[dstPos + 3] = aBound[srcPos + 3]
        bounds[dstPos + 4] = aBound[srcPos + 4]
        bounds[dstPos + 5] = aBound[srcPos + 5]
        indices[dstIdx] = array.indices[srcIdx]
    }

    fun set(dstIdx: Int, array: BvhDataArray, srcIdx: Int) {
        val dstPos = dstIdx * 6
        val srcPos = srcIdx * 6

        val aBound = array.bounds
        val bounds = this.bounds
        bounds[dstPos] = aBound[srcPos]
        bounds[dstPos + 1] = aBound[srcPos + 1]
        bounds[dstPos + 2] = aBound[srcPos + 2]
        bounds[dstPos + 3] = aBound[srcPos + 3]
        bounds[dstPos + 4] = aBound[srcPos + 4]
        bounds[dstPos + 5] = aBound[srcPos + 5]
        indices[dstIdx] = array.data[srcIdx]
    }

    fun getBounds(nodeIndex: Int, out: AABB): AABB {
        val pos = nodeIndex * 6
        out.min.set(bounds[pos], bounds[pos + 1], bounds[pos + 2])
        out.max.set(bounds[pos + 3], bounds[pos + 4], bounds[pos + 5])
        return out
    }

    fun setBounds(nodeIndex: Int, aabb: AABB) {
        val pos = nodeIndex * 6
        bounds[pos] = aabb.min.x
        bounds[pos + 1] = aabb.min.y
        bounds[pos + 2] = aabb.min.z
        bounds[pos + 3] = aabb.max.x
        bounds[pos + 4] = aabb.max.y
        bounds[pos + 5] = aabb.max.z
    }

    fun isLeafNode(nodeIndex: Int): Boolean {
        // skipIndex is negative (internal node), triangleIndex >=0 (leaf node)
        return (indices[nodeIndex] >= 0)
    }

    fun getEscapeIndex(nodeIndex: Int): Int {
        //btAssert(m_escapeIndexOrDataIndex < 0);
        return -indices[nodeIndex]
    }

    fun setEscapeIndex(nodeIndex: Int, index: Int) {
        indices[nodeIndex] = -index
    }

    fun getDataIndex(nodeIndex: Int): Int {
        //btAssert(m_escapeIndexOrDataIndex >= 0);
        return indices[nodeIndex]
    }

    fun setDataIndex(nodeIndex: Int, index: Int) {
        indices[nodeIndex] = index
    }
}
