package me.anno.utils.test

import me.anno.audio.effects.Domain
import me.anno.audio.effects.Time
import me.anno.audio.effects.impl.EqualizerEffect
import me.anno.objects.Camera
import me.anno.objects.Video
import me.anno.utils.Maths.pow
import me.anno.utils.OS
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sin

// todo test, how effects influence the looks and sound of waveforms

fun main() {

    val file = File(OS.downloads, "Aitana 11 Raizones.mp4")
    if (!file.exists()) throw RuntimeException("Missing file!")

    val audio = Video(file)
    val camera = Camera()
    val effect = EqualizerEffect()
    val effects = audio.effects
    effects.stages += effect
    effects.audio = audio
    effects.camera = camera
    /*val component = BufferStream(audio, 48000, camera)
    thread { component.requestNextBuffer(0.0, 0) }
    val buffer = component.getAndReplace()
    for (i in 0 until 1000) {
        println(buffer[i * 2 + 48000])
    }*/

    for(i in effect.frequencies.indices){
        effect.sliders[i].set(Math.random().toFloat()) // 0.37457f
    }

    val func = FloatArray(1024*2) { pow(sin(it * Math.PI / 9f).toFloat()*9f, 3f) }
    val time = func.size / 48000.0

    fun print(data: FloatArray){
        val size = 16
        println("${(0 until size).joinToString { data[1024-size+it].roundToInt().toString() }} | ${(0 until size).joinToString { data[1024+it].roundToInt().toString() }}")
    }

    print(func)
    val result = effects.process(func, Domain.TIME_DOMAIN, Domain.TIME_DOMAIN, Time(0.0, 0.0), Time(time, time))
    print(result)

}
