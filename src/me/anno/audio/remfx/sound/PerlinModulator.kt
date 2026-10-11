package me.anno.audio.remfx.sound

import me.anno.maths.Maths.mix
import me.anno.maths.noise.PerlinNoise
import me.anno.audio.remfx.RemFXContext
import me.anno.audio.remfx.effect.Sound

/**
 * Used to create wind-background noise.
 * */
class PerlinModulator(sound: Sound) : Modulator(sound) {

    private val noise = PerlinNoise(1234, 4, 0.5f, 0f, 32e3f)

    var minVolume = 0f
    var maxVolume = 1f

    var ctr = 0f
    var dCtr = 0.01f

    override fun update(context: RemFXContext) {
        left = mix(minVolume, maxVolume, noise[ctr])
        ctr += dCtr// * bufferSize
        right = left
    }
}