package com.bulletphysics.extras.gimpact

import org.joml.Vector3d

/**
 * @author jezek2
 */
class BvhDataArray {
    private var size = 0

    var bounds: DoubleArray = DoubleArray(0)
    var data: IntArray = IntArray(0)

    fun size(): Int {
        return size
    }

    fun resize(newSize: Int) {
        val newBound = DoubleArray(newSize * 6)
        val newData = IntArray(newSize)

        System.arraycopy(bounds, 0, newBound, 0, size * 6)
        System.arraycopy(data, 0, newData, 0, size)

        bounds = newBound
        data = newData

        size = newSize
    }

    fun swap(idx1: Int, idx2: Int) {
        val pos1 = idx1 * 6
        val pos2 = idx2 * 6

        val b0 = bounds[pos1]
        val b1 = bounds[pos1 + 1]
        val b2 = bounds[pos1 + 2]
        val b3 = bounds[pos1 + 3]
        val b4 = bounds[pos1 + 4]
        val b5 = bounds[pos1 + 5]
        val d = data[idx1]

        bounds[pos1] = bounds[pos2]
        bounds[pos1 + 1] = bounds[pos2 + 1]
        bounds[pos1 + 2] = bounds[pos2 + 2]
        bounds[pos1 + 3] = bounds[pos2 + 3]
        bounds[pos1 + 4] = bounds[pos2 + 4]
        bounds[pos1 + 5] = bounds[pos2 + 5]
        data[idx1] = data[idx2]

        bounds[pos2] = b0
        bounds[pos2 + 1] = b1
        bounds[pos2 + 2] = b2
        bounds[pos2 + 3] = b3
        bounds[pos2 + 4] = b4
        bounds[pos2 + 5] = b5
        data[idx2] = d
    }

    fun getBounds(idx: Int, out: AABB): AABB {
        val pos = idx * 6
        out.min.set(bounds[pos], bounds[pos + 1], bounds[pos + 2])
        out.max.set(bounds[pos + 3], bounds[pos + 4], bounds[pos + 5])
        return out
    }

    fun getBoundsMin(idx: Int, out: Vector3d): Vector3d {
        val pos = idx * 6
        out.set(bounds[pos], bounds[pos + 1], bounds[pos + 2])
        return out
    }

    fun getBoundsMax(idx: Int, out: Vector3d): Vector3d {
        val pos = idx * 6
        out.set(bounds[pos + 3], bounds[pos + 4], bounds[pos + 5])
        return out
    }

    fun setBounds(idx: Int, aabb: AABB) {
        val pos = idx * 6
        bounds[pos] = aabb.min.x
        bounds[pos + 1] = aabb.min.y
        bounds[pos + 2] = aabb.min.z
        bounds[pos + 3] = aabb.max.x
        bounds[pos + 4] = aabb.max.y
        bounds[pos + 5] = aabb.max.z
    }

    fun getData(idx: Int): Int {
        return data[idx]
    }

    fun setData(idx: Int, value: Int) {
        data[idx] = value
    }
}
