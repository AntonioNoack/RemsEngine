package org.recast4j

class IntArray2D(val sizeX: Int, val sizeY: Int) {
    private val content = IntArray(sizeX * sizeY)

    private fun getIndex(x: Int, y: Int): Int {
        return x + y * sizeX
    }

    operator fun get(x: Int, y: Int): Int {
        return content[getIndex(x, y)]
    }

    operator fun set(x: Int, y: Int, v: Int) {
        content[getIndex(x, y)] = v
    }
}