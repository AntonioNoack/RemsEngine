package me.anno.audio.effects.falloff

import me.anno.audio.effects.Domain
import me.anno.audio.effects.SoundEffect
import me.anno.audio.effects.SoundPipeline.Companion.bufferSize
import me.anno.audio.effects.Time
import me.anno.io.base.BaseWriter
import me.anno.remsstudio.objects.Audio
import me.anno.remsstudio.objects.Camera
import me.anno.animation.Type
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.editor.SettingCategory
import me.anno.ui.style.Style
import me.anno.maths.Maths.mix
import org.joml.Vector3f

abstract class Falloff : SoundEffect(Domain.TIME_DOMAIN, Domain.TIME_DOMAIN) {

    var halfDistance = 1f

    abstract fun getAmplitude(relativeDistance: Float): Float

    override fun getStateAsImmutableKey(source: Audio, destination: Camera, time0: Time, time1: Time): Any {
        val amplitude0 = getAmplitude(source, destination, time0.globalTime)
        val amplitude1 = getAmplitude(source, destination, time1.globalTime)
        return Pair(amplitude0, amplitude1)
    }

    fun getAmplitude(source: Audio, destination: Camera, globalTime: Double): Float {
        val position = source.getGlobalTransformTime(globalTime).first.transformPosition(Vector3f())
        val camera = destination.getGlobalTransformTime(globalTime).first.transformPosition(Vector3f())
        val distance = camera.distance(position)
        return getAmplitude(distance / halfDistance)
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
        val amplitude0 = getAmplitude(source, destination, time0.globalTime)
        val amplitude1 = getAmplitude(source, destination, time1.globalTime)
        for (i in 0 until bufferSize) {
            val amplitude = mix(amplitude0, amplitude1, (i + 0.5f) / bufferSize)
            dataDst[i] = dataSrc[i] * amplitude
        }
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
        getGroup: (title: String, description: String, dictSubPath: String) -> SettingCategory
    ) {
        list.add(audio.vi(
            "Half Distance",
            "Distance, where the amplitude is 50%",
            Type.FLOAT_PLUS_EXP,
            halfDistance,
            style
        ) { halfDistance = it })
    }

}