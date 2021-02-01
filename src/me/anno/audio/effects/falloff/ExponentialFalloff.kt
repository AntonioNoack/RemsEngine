package me.anno.audio.effects.falloff

import me.anno.audio.effects.SoundEffect
import me.anno.utils.Maths.pow

class ExponentialFalloff() : Falloff() {

    constructor(halfDistance: Float) : this() {
        this.halfDistance = halfDistance
    }

    override fun getAmplitude(relativeDistance: Float): Float {
        return pow(0.5f, relativeDistance)
    }

    override val displayName: String = "Exponential Falloff"
    override val description: String = "Sound falloff ~ 0.5 ^ distance"
    override fun clone(): SoundEffect =
        ExponentialFalloff(halfDistance)

    override fun getClassName() = "ExponentialFalloffEffect"

}