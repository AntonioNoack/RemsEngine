package me.anno.remsstudio.audio.effects.falloff

import me.anno.remsstudio.audio.effects.SoundEffect

class SquareFalloff() : Falloff() {

    constructor(halfDistance: Float) : this() {
        this.halfDistance = halfDistance
    }

    override fun getAmplitude(relativeDistance: Float): Float {
        return 1f / (1f + relativeDistance * relativeDistance)
    }

    override val displayName: String = "Square Falloff"
    override val description: String = "Sound falloff ~ 1/(1+distance²)"
    override fun clone(): SoundEffect = SquareFalloff(halfDistance)

    override val className get() = "SquareFalloffEffect"

}