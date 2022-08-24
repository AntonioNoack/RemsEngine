package org.joml

class Planef(var a: Float, var b: Float, var c: Float, var d: Float) {

    constructor() : this(0f, 0f, 0f, 0f)

    constructor(pos: Vector3f, dir: Vector3f) :
            this(dir.x, dir.y, dir.z, -pos.dot(dir))

    fun set(x: Float, y: Float, z: Float, w: Float): Planef {
        a = x
        b = y
        c = z
        d = w
        return this
    }
}