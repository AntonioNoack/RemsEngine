package me.anno.audio.effects.impl

import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.SoundPipeline.Companion.bufferSizeM1
import me.anno.audio.effects.Time
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.clamp
import me.anno.utils.processBalanced
import java.util.*
import kotlin.math.log2
import kotlin.math.min

class EchoEffect() : SoundEffect(Domain.TIME_DOMAIN, Domain.TIME_DOMAIN) {

    constructor(audio: Audio) : this() {
        this.audio = audio
    }

    private val recentData = ArrayList<FloatArray>(maxBuffers)

    var offset = AnimatedProperty.floatPlus(0.1f) // seconds
    var falloff = AnimatedProperty.float01exp(0.4f)

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        list += audio.VI("Offset", "Distance of 1st echo in seconds", offset, style)
        list += audio.VI("Falloff", "How much is reflected, the less, the faster the echo fades away", falloff, style)
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "falloff", falloff)
        writer.writeObject(this, "offset", offset)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "falloff" -> falloff.copyFrom(value)
            "offset" -> offset.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    companion object {

        val maxEchoes = 64
        val randomizedOffsets: FloatArray
        private val maxBuffers = 128

        init {
            val random = Random(1234)
            randomizedOffsets = FloatArray(maxEchoes) { (it + 1) * (0.9f + 0.2f) * random.nextFloat() }
        }

        val minRelativeAmplitude = 0.001f

    }

    fun put(data: FloatArray) {
        if (recentData.size >= maxBuffers) {
            recentData.removeAt(0)
        }
        val copy = FloatArray(bufferSize)
        System.arraycopy(data, 0, copy, 0, bufferSize)
        recentData += copy
    }

    override fun apply(data: FloatArray, source: Audio, destination: Camera, time0: Time, time1: Time): FloatArray {

        val bufferSize = bufferSize
        val bufferSizeM1 = bufferSizeM1

        val time = (time0.localTime + time1.localTime) * 0.5
        val falloff = falloff[time]
        val echoes = min(maxEchoes, (log2(minRelativeAmplitude) / log2(falloff)).toInt())
        val bufferIndex = recentData.lastIndex
        val offset0 = offset[time] * bufferSize / (time1.localTime - time0.localTime)

        put(data)

        if (echoes > 0 && offset0 > 0.5) {

            val knownSampleCount = recentData.size * bufferSize

            var multiplier = 1f
            for (echo in 0 until echoes) {
                multiplier *= falloff
                val offset = (offset0 * randomizedOffsets[echo]).toInt()
                val startIndex0 = bufferIndex * bufferSize - offset
                val startIndex = clamp(startIndex0, 0, knownSampleCount)
                val endIndex = clamp(startIndex0 + bufferSize, 0, knownSampleCount)
                // we read at max from two buffers
                if (startIndex < knownSampleCount && endIndex > 0) {
                    val buffer0 = recentData[startIndex / bufferSize]
                    val buffer1 = recentData[(endIndex - 1) / bufferSize]
                    val midIndex = min((startIndex / bufferSize + 1) * bufferSize, endIndex)
                    processBalanced(startIndex, midIndex, false) { i0, i1 ->
                        for (i in i0 until i1) {
                            data[i - startIndex0] += buffer0[i and bufferSizeM1]
                        }
                    }
                    processBalanced(midIndex, endIndex, false) { i0, i1 ->
                        for (i in i0 until i1) {
                            data[i - startIndex0] += buffer1[i and bufferSizeM1]
                        }
                    }
                }
            }

            var totalMultiplier = 1f
            multiplier = 1f
            for (echo in 0 until echoes) {
                multiplier *= falloff
                totalMultiplier += multiplier
            }

            totalMultiplier = 1f / totalMultiplier

            for (i in 0 until bufferSize) {
                data[i] *= totalMultiplier
            }

        }

        return data

    }

    override fun clone(): SoundEffect {
        val clone = EchoEffect()
        clone.offset = offset
        clone.falloff = falloff
        return clone
    }

    override val displayName: String = "Echo Effect"
    override val description: String = "Repeats the sound"
    override fun getClassName() = "EchoEffect"

}