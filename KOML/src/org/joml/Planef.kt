package org.joml

open class Planef(
    @JvmField var dirX: Float,
    @JvmField var dirY: Float,
    @JvmField var dirZ: Float,
    @JvmField var distance: Float
) : Vector() {

    constructor() : this(0f, 0f, 0f, 0f)

    constructor(pos: Vector3f, dir: Vector3f) :
            this(dir.x, dir.y, dir.z, -pos.dot(dir))

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = when (i) {
        0 -> dirX
        1 -> dirY
        2 -> dirZ
        else -> distance
    }.toDouble()

    override fun setComp(i: Int, v: Double) {
        val vf = v.toFloat()
        when (i) {
            0 -> dirX = vf
            1 -> dirY = vf
            2 -> dirZ = vf
            else -> distance = vf
        }
    }

    fun set(x: Float, y: Float, z: Float, w: Float): Planef {
        dirX = x
        dirY = y
        dirZ = z
        distance = w
        return this
    }

    fun set(pos: Vector3f, dir: Vector3f) = set(dir.x, dir.y, dir.z, -pos.dot(dir))
    fun set(src: Planef): Planef = set(src.dirX, src.dirY, src.dirZ, src.distance)

    fun dot(x: Float, y: Float, z: Float): Float = x * dirX + y * dirY + z * dirZ + distance
    fun dot(v: Vector3f): Float = dot(v.x, v.y, v.z)

    fun findX(y: Float, z: Float): Float = -dot(0f, y, z) / dirX
    fun findY(x: Float, z: Float): Float = -dot(x, 0f, z) / dirY
    fun findZ(x: Float, y: Float): Float = -dot(x, y, 0f) / dirZ

    override fun equals(other: Any?): Boolean {
        return other is Planef &&
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