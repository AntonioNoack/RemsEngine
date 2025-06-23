package me.anno.maths.geometry.convexhull

import org.joml.Vector3i

/**
 * @author jezek2
 */
internal class Triangle(a: Int, b: Int, c: Int) : Vector3i(a, b, c) {

    val n = Vector3i(-1, -1, -1)
    var id: Int = 0
    var maxValue: Int = -1
    var rise: Double = 0.0

    fun getNeighbor(a: Int, b: Int): Int {
        val x = x
        val y = y
        val z = z
        return if ((x == a && y == b) || (x == b && y == a)) {
            n.z
        } else if ((y == a && z == b) || (y == b && z == a)) {
            n.x
        } else {
            n.y
        }
    }

    fun setNeighbor(a: Int, b: Int, value: Int) {
        val x = x
        val y = y
        val z = z
        if ((x == a && y == b) || (x == b && y == a)) {
            n.z = value
        } else if ((y == a && z == b) || (y == b && z == a)) {
            n.x = value
        } else {
            n.y = value
        }
    }
}
