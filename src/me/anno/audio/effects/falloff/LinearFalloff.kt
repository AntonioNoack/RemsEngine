package me.anno.audio.effects.falloff

import me.anno.audio.effects.SoundEffect
import me.anno.objects.Audio
import kotlin.math.max

class LinearFalloff() : Falloff() {

    constructor(halfDistance: Float) : this() {
        this.halfDistance = halfDistance
    }

    override fun getAmplitude(relativeDistance: Float): Float {
        return max(0f, 1f - 0.5f * relativeDistance)
    }

    override val displayName: String = "Linear Falloff"
    override val description: String = "Sound falloff ~ 1-distance"
    override fun clone(): SoundEffect = LinearFalloff(halfDistance)

    override fun getClassName() = "LinearFalloffEffect"

}