package me.anno.audio.remfx.modulator

import me.anno.audio.remfx.RemFXContext
import me.anno.audio.remfx.effect.Sound
import kotlin.math.max

open class Modulator(var sound: Sound) : Sound {

    companion object {
        var minAmplitude = 10f
    }

    var left = 20e3f
    var right = 20e3f

    var lastLeft = 0f
    var lastRight = 0f

    fun setVolume(left: Float, right: Float = left): Modulator {
        this.left = left
        this.right = right
        return this
    }

    override fun hasTimeDelay(): Boolean = sound.hasTimeDelay()

    override fun add(context: RemFXContext, left: ShortArray, right: ShortArray) {
        val l = lastLeft
        val r = lastRight
        context.amplitudeL0 = l
        context.amplitudeR0 = r

        val invSize = 1f / context.bufferSize
        context.deltaAmplitudeL = (this.left - l) * invSize
        context.deltaAmplitudeR = (this.right - r) * invSize

        val sound = sound
        val maxAmplitude = max(max(this.left, l), max(this.right, r))
        if (maxAmplitude >= minAmplitude || sound.hasTimeDelay()) {
            sound.update(context)
            sound.add(context, left, right)
        }

        lastLeft = this.left
        lastRight = this.right
    }
}