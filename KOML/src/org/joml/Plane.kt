package org.joml

import org.joml.JomlMath.hash

/**
 * Plane with mixed precision
 * */
open class Plane(
    @JvmField var dirX: Float,
    @JvmField var dirY: Float,
    @JvmField var dirZ: Float,
    @JvmField var distance: Double
) : Vector {

    constructor() : this(0f, 0f, 0f, 0.0)

    constructor(pos: Vector3d, dir: Vector3f) :
            this(dir.x, dir.y, dir.z, -pos.dot(dir))

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = when (i) {
        0 -> dirX.toDouble()
        1 -> dirY.toDouble()
        2 -> dirZ.toDouble()
        else -> distance
    }

    override fun setComp(i: Int, v: Double) {
        when (i) {
            0 -> dirX = v.toFloat()
            1 -> dirY = v.toFloat()
            2 -> dirZ = v.toFloat()
            else -> distance = v
        }
    }

    fun set(x: Float, y: Float, z: Float, w: Double): Plane {
        dirX = x
        dirY = y
        dirZ = z
        distance = w
        return this
    }

    fun set(pos: Vector3d, dir: Vector3f) = set(dir.x, dir.y, dir.z, -pos.dot(dir))
    fun set(src: Plane): Plane = set(src.dirX, src.dirY, src.dirZ, src.distance)

    fun dot(x: Double, y: Double, z: Double): Double = x * dirX + y * dirY + z * dirZ + distance
    fun dot(v: Vector3d): Double = dot(v.x, v.y, v.z)

    /**
     * given y and z, calculates x
     * */
    fun findX(y: Double, z: Double): Double = -dot(0.0, y, z) / dirX

    /**
     * given x and z, calculates y
     * */
    fun findY(x: Double, z: Double): Double = -dot(x, 0.0, z) / dirY

    /**
     * given x and y, calculates z
     * */
    fun findZ(x: Double, y: Double): Double = -dot(x, y, 0.0) / dirZ

    override fun equals(other: Any?): Boolean {
        return other is Plane &&
                dirX == other.dirX &&
                dirY == other.dirY &&
                dirZ == other.dirZ &&
                distance == other.distance
    }

    override fun hashCode(): Int {
        var hash = hash(dirX)
        hash = hash * 31 + hash(dirY)
        hash = hash * 31 + hash(dirZ)
        hash = hash * 31 + hash(distance)
        return hash
    }

    override fun toString(): String {
        return "Plane($dirX, $dirY, $dirZ, $distance)"
    }
}