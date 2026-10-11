package me.anno.audio.remfx.sound

import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.dtTo10
import me.anno.audio.remfx.RemFXContext
import me.anno.audio.remfx.effect.Sound
import kotlin.math.min

class PlayedModulator(var key: Key, sound: Sound) : Modulator(sound) {

    var amplitude = 0f
    var raise = 0f
    var defaultVolume = 20e3f
    var falloff = 5f

    override fun update(context: RemFXContext) {

        if (Input.isKeyDown(key)) {
            raise = amplitude
            amplitude = 1f
        } else {
            val dt = context.bufferSize.toFloat() / context.sampleRate
            raise = min(raise + dt * 30f, 1f)
            amplitude *= dtTo10(dt * falloff)
        }

        setVolume(defaultVolume * amplitude * raise)
    }
}