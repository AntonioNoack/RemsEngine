package com.bulletphysics.linearmath.convexhull

/**
 * @author jezek2
 */
internal open class Int3 {
    var x: Int = 0
    var y: Int = 0
    var z: Int = 0

    constructor()

    constructor(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    constructor(i: Int3) {
        x = i.x
        y = i.y
        z = i.z
    }

    fun set(x: Int, y: Int, z: Int) {
        this.x = x
        this.y = y
        this.z = z
    }

    fun getCoord(coord: Int): Int {
        when (coord) {
            0 -> return x
            1 -> return y
            else -> return z
        }
    }
}
