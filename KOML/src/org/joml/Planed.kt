package org.joml

open class Planed(var a: Double, var b: Double, var c: Double, var d: Double) {

    constructor() : this(0.0, 0.0, 0.0, 0.0)

    constructor(pos: Vector3d, dir: Vector3d) :
            this(dir.x, dir.y, dir.z, -pos.dot(dir))

    fun set(x: Double, y: Double, z: Double, w: Double): Planed {
        a = x
        b = y
        c = z
        d = w
        return this
    }
}