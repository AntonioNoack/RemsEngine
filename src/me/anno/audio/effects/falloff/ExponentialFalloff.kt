package me.anno.audio.effects.falloff

import me.anno.utils.maths.Maths.pow

class ExponentialFalloff() : Falloff() {

    constructor(halfDistance: Float) : this() {
        this.halfDistance = halfDistance
    }

    override fun getAmplitude(relativeDistance: Float): Float {
        return pow(0.5f, relativeDistance)
    }

    override val displayName: String = "Exponential Falloff"
    override val description: String = "Sound falloff ~ 0.5 ^ distance"

    override val className get() = "ExponentialFalloffEffect"

}