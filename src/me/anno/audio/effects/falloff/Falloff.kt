package me.anno.audio.effects.falloff

import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.Time
import me.anno.io.base.BaseWriter
import me.anno.objects.Audio
import me.anno.objects.Camera
import me.anno.objects.animation.Type
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.utils.Maths.mix
import org.joml.Vector3f

abstract class Falloff() : SoundEffect(Domain.TIME_DOMAIN, Domain.TIME_DOMAIN) {

    constructor(audio: Audio): this() {
        this.audio = audio
    }

    constructor(halfDistance: Float) : this() {
        this.halfDistance = halfDistance
    }

    var halfDistance = 1f

    abstract fun getAmplitude(relativeDistance: Float): Float

    fun getAmplitude(source: Audio, destination: Camera, globalTime: Double): Float {
        val position = source.getGlobalTransform(globalTime).first.transformPosition(Vector3f())
        val camera = destination.getGlobalTransform(globalTime).first.transformPosition(Vector3f())
        val distance = camera.distance(position)
        return getAmplitude(distance/halfDistance)
    }

    override fun apply(data: FloatArray, source: Audio, destination: Camera, time0: Time, time1: Time): FloatArray {
        val amplitude0 = getAmplitude(source, destination, time0.globalTime)
        val amplitude1 = getAmplitude(source, destination, time1.globalTime)
        for (i in 0 until bufferSize) {
            val amplitude = mix(amplitude0, amplitude1, (i + 0.5f) / bufferSize)
            data[i] *= amplitude
        }
        return data
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("halfDistance", halfDistance)
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "halfDistance" -> halfDistance = value
            else -> super.readFloat(name, value)
        }
    }

    override fun createInspector(
        list: PanelListY,
        style: Style,
        getGroup: (title: String, id: String) -> SettingCategory
    ) {
        list += audio.vi(
            "Half Distance",
            "Distance, where the amplitude is 50%",
            Type.FLOAT_PLUS_EXP,
            halfDistance,
            style
        ) { halfDistance = it }
    }

}