package me.anno.maths.noise

import org.joml.Vector3f

class BlueNoiseSampler3f(size: Vector3f, minDist: Float, maxAttempts: Int, seed: Long) :
    BlueNoiseSampler<Vector3f>(size, minDist, maxAttempts, seed) {

    override fun generatePoint() = Vector3f()
    override fun distanceSquared(a: Vector3f, b: Vector3f): Float = a.distanceSquared(b)
}
