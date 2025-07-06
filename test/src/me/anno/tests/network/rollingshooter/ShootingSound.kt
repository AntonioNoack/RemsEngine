package me.anno.tests.network.rollingshooter

import me.anno.ecs.components.audio.AudioComponent
import me.anno.io.files.inner.temporary.InnerTmpAudioFile
import me.anno.maths.noise.FullNoise
import kotlin.math.exp
import kotlin.random.Random

class ShootingSound(seed: Long) : InnerTmpAudioFile() {

    override val sampleCount: Long
        get() = (sampleRate / 2).toLong() // half of a second

    private val noise = FullNoise(seed)
    override fun sample(time: Double, channel: Int): Short {
        val falloff = exp(-10.0 * time)
        val noise = (noise[time * 10000] * 2.0 - 1.0) * falloff
        val volume = 32767
        return (noise * volume).toInt().toShort()
    }

    companion object {
        private val rnd = Random(1653)
        val sounds = Array(10) {
            ShootingSound(rnd.nextLong())
        }
        val audios = sounds.map { source ->
            val audio = AudioComponent()
            audio.source = source
            audio.rollOffFactor = 0f // global
            audio
        }
    }
}