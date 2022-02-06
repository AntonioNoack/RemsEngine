package me.anno.remsstudio.audio.effects.impl

import me.anno.audio.AudioStreamRaw.Companion.bufferSize
import me.anno.remsstudio.audio.effects.Domain
import me.anno.remsstudio.audio.effects.SoundEffect
import me.anno.remsstudio.audio.effects.Time
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.remsstudio.objects.Audio
import me.anno.remsstudio.objects.Camera
import me.anno.remsstudio.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.pow
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import kotlin.math.abs
import kotlin.math.log2

class EqualizerEffect : SoundEffect(Domain.FREQUENCY_DOMAIN, Domain.FREQUENCY_DOMAIN) {

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        // todo better equalizer view...
        frequencies.forEachIndexed { index, frequency ->
            list += audio.vi(
                if (frequency > 999) "${frequency / 1000} kHz" else "$frequency Hz",
                "Relative amplitude of this frequency, from -12dB to +12dB", sliders[index], style
            )
        }
    }

    val range = pow(10f, 2.4f) // +/- 12dB

    // val frequencies = 32 .. 16000 = 2 ^ (5 .. 14)
    val frequencies = Array(10) {
        1.shl(it + 5)
    }

    val sliders = Array(frequencies.size) {
        AnimatedProperty.float01(0.5f)
    }

    override fun getStateAsImmutableKey(source: Audio, destination: Camera, time0: Time, time1: Time): Any {
        return sliders.joinToString()
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectArray(this, "sliders", sliders)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "sliders" -> {
                values.forEachIndexed { index, iSaveable ->
                    sliders.getOrNull(index)?.copyFrom(iSaveable)
                }
            }
            else -> super.readObjectArray(name, values)
        }
    }

    fun getAmplitude(frequency: Double, sliders: FloatArray): Float {
        val index = log2(frequency) - 5
        val index0 = clamp(index.toInt(), 0, frequencies.size - 2)
        val index1 = index0 + 1
        return pow(
            range, mix(
                sliders[index0],
                sliders[index1],
                clamp((index - index0).toFloat(), 0f, 1f)
            ) - 0.5f
        )
    }

    override fun apply(
        getDataSrc: (Int) -> FloatArray,
        dataDst: FloatArray,
        source: Audio,
        destination: Camera,
        time0: Time,
        time1: Time
    ) {

        val dataSrc = getDataSrc(0)

        val dt = time1.localTime - time0.localTime
        val time = time0.localTime + dt / 2

        val sliders = sliders.map { it[time] }
        if (sliders.all { abs(it - 0.5) < 1e-3f }) {
            // LOGGER.info("no change at all")
            copy(dataSrc, dataDst)
            return
        }

        val firstSlider = sliders.first()
        if (sliders.all { abs(it - firstSlider) < 1e-3f }) {
            // LOGGER.info("all the same")
            // just multiply everything
            val amplitude = pow(range, firstSlider - 0.5f)
            if (amplitude < 1e-7f) {
                for (i in 0 until bufferSize) dataDst[i] = 0f
            } else {
                for (i in 0 until bufferSize) dataDst[i] = dataSrc[i] * amplitude
            }
        }

        val slidersArray = sliders.toFloatArray()
        processBalanced(1, bufferSize / 2, 256) { i0, i1 ->
            for (i in i0 until i1) {
                val frequency = i / dt // in Hz
                val multiplier = getAmplitude(frequency, slidersArray)
                if (multiplier != 1f) {
                    val j = i * 2
                    val k = j + 1
                    dataDst[j] = dataSrc[j] * multiplier
                    dataDst[k] = dataSrc[k] * multiplier
                }
            }
        }

    }

    override val displayName: String = "Equalizer"
    override val description: String = "Changes the volume highs/mids/lows"
    override val className get() = "EqualizerEffect"

}