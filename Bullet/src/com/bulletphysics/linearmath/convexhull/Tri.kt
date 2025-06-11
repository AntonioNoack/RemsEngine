package com.bulletphysics.linearmath.convexhull

/**
 * @author jezek2
 */
internal class Tri(a: Int, b: Int, c: Int) : Int3(a, b, c) {
    var n: Int3 = Int3()
    var id: Int = 0
    var maxValue: Int
    var rise: Double

    init {
        n.set(-1, -1, -1)
        maxValue = -1
        rise = 0.0
    }

    fun neibGet(a: Int, b: Int): Int {
        val x = this.x
        val y = this.y
        val z = this.z
        if ((x == a && y == b) || (x == b && y == a)) {
            return n.z
        } else if ((y == a && z == b) || (y == b && z == a)) {
            return n.x
        } else {
            return n.y
        }
    }

    fun neibSet(a: Int, b: Int, value: Int) {
        val x = this.x
        val y = this.y
        val z = this.z
        if ((x == a && y == b) || (x == b && y == a)) {
            n.z = value
        } else if ((y == a && z == b) || (y == b && z == a)) {
            n.x = value
        } else {
            n.y = value
        }
    }
}
