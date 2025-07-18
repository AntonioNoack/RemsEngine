package org.joml

open class Planed(
    @JvmField var dirX: Double,
    @JvmField var dirY: Double,
    @JvmField var dirZ: Double,
    @JvmField var distance: Double
) : Vector {

    constructor() : this(0.0, 0.0, 0.0, 0.0)

    constructor(pos: Vector3d, dir: Vector3d) :
            this(dir.x, dir.y, dir.z, -pos.dot(dir))

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = when (i) {
        0 -> dirX
        1 -> dirY
        2 -> dirZ
        else -> distance
    }

    override fun setComp(i: Int, v: Double) {
        when (i) {
            0 -> dirX = v
            1 -> dirY = v
            2 -> dirZ = v
            else -> distance = v
        }
    }

    fun set(x: Double, y: Double, z: Double, w: Double): Planed {
        dirX = x
        dirY = y
        dirZ = z
        distance = w
        return this
    }

    fun set(pos: Vector3d, dir: Vector3d) = set(dir.x, dir.y, dir.z, -pos.dot(dir))
    fun set(pos: Vector3d, dir: Vector3f) = set(dir.x.toDouble(), dir.y.toDouble(), dir.z.toDouble(), -pos.dot(dir))
    fun set(src: Planed): Planed = set(src.dirX, src.dirY, src.dirZ, src.distance)

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
        return other is Planed &&
                dirX == other.dirX &&
                dirY == other.dirY &&
                dirZ == other.dirZ &&
                distance == other.distance
    }

    override fun hashCode(): Int {
        var hash = dirX.hashCode()
        hash = hash * 31 + dirY.hashCode()
        hash = hash * 31 + dirZ.hashCode()
        hash = hash * 31 + distance.hashCode()
        return hash
    }

    override fun toString(): String {
        return "Plane($dirX, $dirY, $dirZ, $distance)"
    }
}