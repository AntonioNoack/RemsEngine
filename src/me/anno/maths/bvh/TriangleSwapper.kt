package me.anno.maths.bvh

import me.anno.utils.search.Swapper

class TriangleSwapper(val indices: IntArray) : Swapper {
    override fun swap(i: Int, j: Int) {
        val i3 = i * 3
        val j3 = j * 3
        indices.swap(i3, j3)
        indices.swap(i3 + 1, j3 + 1)
        indices.swap(i3 + 2, j3 + 2)
    }

    fun IntArray.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}