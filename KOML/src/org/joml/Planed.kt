package org.joml

open class Planed(
    @JvmField var dirX: Double,
    @JvmField var dirY: Double,
    @JvmField var dirZ: Double,
    @JvmField var distance: Double
) {

    constructor() : this(0.0, 0.0, 0.0, 0.0)

    constructor(pos: Vector3d, dir: Vector3d) :
            this(dir.x, dir.y, dir.z, -pos.dot(dir))

    fun set(x: Double, y: Double, z: Double, w: Double): Planed {
        dirX = x
        dirY = y
        dirZ = z
        distance = w
        return this
    }

    fun set(pos: Vector3d, dir: Vector3d) = set(dir.x, dir.y, dir.z, -pos.dot(dir))

    fun set(src: Planed): Planed = set(src.dirX, src.dirY, src.dirZ, src.distance)

    fun dot(x: Double, y: Double, z: Double): Double = x * dirX + y * dirY + z * dirZ + distance

    fun dot(v: Vector3d): Double = dot(v.x, v.y, v.z)

    override fun toString(): String {
        return "Plane($dirX, $dirY, $dirZ, $distance)"
    }
}