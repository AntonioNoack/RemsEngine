package me.anno.utils.maths.geometry

import org.joml.Vector3f
import org.joml.Vector4f

class Plane3D(val origin: Vector3f, val normal: Vector3f) : Vector4f() {

    constructor(p: Plane3D) : this(Vector3f(p.origin), Vector3f(p.normal))

    init {
        set(normal, origin.dot(normal))
    }

    fun isRight(p: Vector3f): Float {
        return p.x * x + p.y * y + p.z * z - w
    }

    operator fun plus(delta: Vector3f): Plane3D {
        val clone = Plane3D(this)
        clone.w += delta.x * x + delta.y * y + delta.z * z // += delta dot normal
        clone.origin.add(delta)
        return clone
    }

}
