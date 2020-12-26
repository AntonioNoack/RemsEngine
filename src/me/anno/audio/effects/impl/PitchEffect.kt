package me.anno.audio.effects.impl

import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.Time
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.animation.AnimatedProperty
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths
import me.anno.utils.processBalanced
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos

class PitchEffect() : SoundEffect(Domain.FREQUENCY_DOMAIN, Domain.TIME_DOMAIN) {

    constructor(audio: Audio) : this() {
        this.audio = audio
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        list += audio.vi(
            "Inverse Speed",
            "Making something play faster, increases the pitch; this is undone by this node",
            null, inverseSpeed, style
        ) { inverseSpeed = it }
        list += audio.vi("Value", "Pitch height, if Inverse Speed = false", pitch, style)
    }

    companion object {
        private val amplitudeMultiplier = 2f / bufferSize
        private val piX2 = (2.0 * Math.PI).toFloat()
    }

    var inverseSpeed = false
    var pitch = AnimatedProperty.float(1f)

    private val phaseOffset = FloatArray(bufferSize / 2)

    override fun reset() {
        for (i in phaseOffset.indices) phaseOffset[i] = 0f
    }

    val result = FloatArray(bufferSize)

    override fun apply(data: FloatArray, source: Audio, destination: Camera, time0: Time, time1: Time): FloatArray {

        val time = source.timeAnimated
        val pitch = pitch

        val isFirstBuffer = bufferIndex == 0

        val timeIntegral = FloatArray(bufferSize)

        if (inverseSpeed) {
            for (i in 0 until bufferSize) {
                val parentTime = time0.globalTime + i * (time1.globalTime - time0.globalTime) / bufferSize
                timeIntegral[i] = source.getLocalTime(parentTime).toFloat()
            }
        } else {
            val t0 = time0.localTime
            val t1 = time1.localTime
            timeIntegral[0] = pitch[t0]
            for (i in 1 until bufferSize) {
                timeIntegral[i] = timeIntegral[i - 1] + pitch[t0 + i * (t1 - t0) / bufferSize]
            }
        }

        for (i in 0 until bufferSize) {
            result[i] = 0f
        }

        // is it possible to work in the frequency domain only? mhh...
        // only approximately
        for (frequency in 1 until bufferSize / 2) {
            val r = data[frequency * 2]
            val i = data[frequency * 2 + 1]
            val amplitude0 = Maths.length(r, i) * amplitudeMultiplier
            if (abs(amplitude0) > 1e-5f && frequency == 4) {
                val baseFrequency = (frequency * 2 * PI).toFloat() / bufferSize
                val phase0 = if (isFirstBuffer) atan2(i, r) else phaseOffset[frequency]
                processBalanced(0, bufferSize, false){ i0, i1 ->
                    for(t in i0 until i1){
                        result[t] += cos((phase0 + timeIntegral[t]) * baseFrequency) * amplitude0
                    }
                }
                phaseOffset[frequency] = (phase0 + timeIntegral.last()) % (piX2 / baseFrequency)
            } else phaseOffset[frequency] = 0f
        }

        bufferIndex++

        return result

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeBoolean("inverseSpeed", inverseSpeed)
        writer.writeObject(this, "pitch", pitch)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "inverseSpeed" -> inverseSpeed = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "pitch" -> pitch.copyFrom(value)
            else -> super.readObject(name, value)
        }
    }

    override fun clone(): SoundEffect {
        val clone = PitchEffect()
        clone.inverseSpeed = inverseSpeed
        clone.pitch = pitch
        return clone
    }

    override val displayName: String = "Pitch Change"
    override val description: String = "Changes how high the frequencies are"
    override fun getClassName() = "PitchEffect"

}