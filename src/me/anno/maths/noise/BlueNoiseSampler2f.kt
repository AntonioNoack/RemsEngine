package me.anno.maths.noise

import org.joml.Vector2f

class BlueNoiseSampler2f(size: Vector2f, minDist: Float, maxAttempts: Int, seed: Long) :
    BlueNoiseSampler<Vector2f>(size, minDist, maxAttempts, seed) {

    override fun generatePoint() = Vector2f()
    override fun distanceSquared(a: Vector2f, b: Vector2f): Float = a.distanceSquared(b)
}
