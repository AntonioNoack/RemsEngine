package me.anno.audio.effects.impl

import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.Time
import me.anno.io.text.TextReader
import me.anno.objects.Audio
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.mix
import me.anno.utils.Maths.pow
import me.anno.utils.processBalanced
import kotlin.math.log2

class EqualizerEffect() : SoundEffect(Domain.FREQUENCY_DOMAIN, Domain.FREQUENCY_DOMAIN) {

    constructor(audio: Audio) : this() {
        this.audio = audio
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        // todo better equalizer view...
        frequencies.forEachIndexed { index, frequency ->
            list += audio.VI(
                if (frequency > 999) "${frequency / 1000} kHz" else "$frequency Hz",
                "Relative amplitude of this frequency, from -12dB to +12dB", sliders[index], style
            )
        }
    }

    val range = pow(10f, 2.4f) // +/- 12dB

    // val frequencies = 32 .. 16000 = 2 ^ (5 .. 16)
    val frequencies = Array(12) {
        1.shl(it + 5)
    }

    val sliders = Array(frequencies.size) {
        AnimatedProperty.float01(0.5f)
    }

    fun getAmplitude(time: Double, frequency: Double): Float {
        val index = log2(frequency) - 5
        val index0 = clamp(index.toInt(), 0, frequencies.size - 2)
        val index1 = index0 + 1
        return pow(
            range, mix(
                sliders[index0][time],
                sliders[index1][time],
                (index - index0).toFloat()
            ) - 0.5f
        )
    }

    override fun apply(data: FloatArray, sound: Audio, time0: Time, time1: Time): FloatArray {

        val dt = time1.localTime - time0.localTime
        val time = time0.localTime + dt / 2

        processBalanced(1, bufferSize / 2, false) { i0, i1 ->
            for (i in i0 until i1) {
                val frequency = i / dt // in Hz
                val multiplier = getAmplitude(time, frequency)
                data[i * 2] *= multiplier
                data[i * 2 + 1] *= multiplier
            }
        }

        return data

    }

    override fun clone(): SoundEffect {
        return TextReader.fromText(toString()).first() as SoundEffect
    }

    override val displayName: String = "Equalizer"
    override val description: String = "Changes the volume highs/mids/lows"
    override fun getClassName() = "EqualizerEffect"

}