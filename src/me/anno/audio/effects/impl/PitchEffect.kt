package me.anno.audio.effects.impl

import audacity.soundtouch.TimeDomainStretch
import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.Time
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.animation.Type
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Casting.castToFloat2
import me.anno.utils.Maths.clamp
import me.anno.utils.Maths.fract
import me.anno.utils.Maths.mix
import kotlin.math.*

class PitchEffect() : SoundEffect(Domain.TIME_DOMAIN, Domain.TIME_DOMAIN) {

    companion object {
        val maxPitch = 20f
        val minPitch = 1f/ maxPitch
        val pitchType = Type(1f, 1, 1f, false, true,
            { clamp(castToFloat2(it), minPitch, maxPitch) },
            { it is Float }
        )
    }

    constructor(audio: Audio) : this() {
        this.audio = audio
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list += audio.vi(
            "Inverse Speed",
            "Making something play faster, increases the pitch; this is undone by this node",
            null, inverseSpeed, style
        ) { inverseSpeed = it }
        list += audio.vi("Value", "Pitch height, if Inverse Speed = false", pitchType, pitch, style){ pitch = it }
    }

    var inverseSpeed = false
    var pitch = 1f

    private val phaseOffset = FloatArray(bufferSize / 2)

    override fun reset() {
        for (i in phaseOffset.indices) phaseOffset[i] = 0f
    }

    val result = FloatArray(bufferSize)

    private val stretch = TimeDomainStretch()
    init { stretch.setChannels(1) }

    var tempo = 1f
    var hasTempo = false
    var outputOffset = 0

    override fun apply(data: FloatArray, source: Audio, destination: Camera, time0: Time, time1: Time): FloatArray {

        if(!hasTempo){
            // todo can tempo be changed while running???...
            tempo = clamp(
                if(inverseSpeed){
                    val localDt = abs(time1.localTime-time0.localTime)
                    val globalDt = abs(time1.globalTime - time0.globalTime)
                    (localDt/globalDt).toFloat()
                } else 1f/pitch, minPitch, maxPitch
            )
            stretch.setTempo(tempo)
            hasTempo = true
        }

        // nothing to do, should be exact enough
        if(tempo in 0.999f .. 1.001f) return data

        // put the data
        stretch.putSamples(data)

        // then read the data, and rescale it to match the output
        val output = stretch.outputBuffer
        val output2 = output.backend
        val offset = outputOffset
        val size = output.numSamples() - offset
        if(size > 0){

            // keep size "constant" (it's not), only use what you need
            val usedSize = min(size, (data.size*pitch).toInt())
            val factor = usedSize.toFloat() / data.size
            // println("$usedSize (${output.numSamples()} - $offset) / ${data.size} -> $factor from $pitch")
            val maxIndex = size + offset - 1

            var f0 = 0f
            var i0 = 0
            for(i in data.indices){
                val f1 = (i+1) * factor
                val i1 = f1.toInt() + offset
                data[i] = if(i1 > i1){
                    // there are multiple values
                    // average f0 .. f1
                    var sum = 0f
                    sum += output2[i0] * (1f-fract(f0))
                    sum += output2[i1] * fract(f1)
                    for(j in i0+1 until i1){
                        sum += output2[j]
                    }
                    sum / (f1-f0)
                } else {
                    // only a single one, lerped
                    mix(output2[i0], output2[min(i0+1, maxIndex)], fract(f0))
                }
                f0 = f1
                i0 = i1
            }

            outputOffset += usedSize

        }

        return data

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeBoolean("inverseSpeed", inverseSpeed)
        writer.writeFloat("pitch", pitch)
        // writer.writeObject(this, "pitch", pitch)
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "inverseSpeed" -> inverseSpeed = value
            else -> super.readBoolean(name, value)
        }
    }

    override fun readFloat(name: String, value: Float) {
        when(name){
            "pitch" -> pitch = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            // "pitch" -> pitch.copyFrom(value)
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