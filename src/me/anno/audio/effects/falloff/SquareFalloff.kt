package me.anno.audio.effects.falloff

import me.anno.audio.effects.SoundEffect
import me.anno.objects.Audio

class SquareFalloff() : Falloff() {

    constructor(audio: Audio) : this() {
        this.audio = audio
    }

    constructor(halfDistance: Float) : this() {
        this.halfDistance = halfDistance
    }

    override fun getAmplitude(relativeDistance: Float): Float {
        return 1f / (1f + relativeDistance * relativeDistance)
    }

    override val displayName: String = "Square Falloff"
    override val description: String = "Sound falloff ~ 1/(1+distance²)"
    override fun clone(): SoundEffect = SquareFalloff(halfDistance)

    override fun getClassName() = "SquareFalloffEffect"

}