package me.anno.audio.test

import me.anno.audio.*
import me.anno.gpu.GFX
import me.anno.video.FFMPEGStream
import org.joml.Vector3f
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

fun main(){

    AudioManager.init()

    try {

        SoundListener.setPosition(Vector3f())
        SoundListener.setVelocity(Vector3f())
        SoundListener.setOrientation(Vector3f(0f, 0f, 1f), Vector3f(0f, 1f, 0f))

        testStream()
        while(true){
            GFX.workQueue(GFX.audioTasks)
        }
        // testSingleBuffer()

        val time0 = System.nanoTime()
        while(true){
            val dt = (System.nanoTime() - time0)*1e-9f
            if(dt > 30) break

            val arc = dt
            val rad = 100f
            SoundListener.setPosition(Vector3f(sin(arc) * rad, 0f, cos(arc) * rad))
            // source.setGain(sin(arc)*0.5f+0.5f)

            Thread.sleep(10)
        }

        // source.destroy()

        // buffer.destroy()

    } finally {
        AudioManager.requestDestruction()
    }

}

val file = File("C:\\Users\\Antonio\\Videos\\Captures\\cool cops\\Watch_Dogs 2 2019-10-14 15-26-49.mp4")

fun testStream(){
    AudioStream(file, 0f).start()
}

fun testSingleBuffer(){
    val buffer2 = FFMPEGStream.getAudioSequence(file, 0f, 10f, 48000)
    var buffer2a: SoundBuffer?
    while(true){
        buffer2a = buffer2.soundBuffer
        if(buffer2a != null) break
        // work on the queue
        GFX.workQueue(GFX.audioTasks)
    }

    val buffer = buffer2a!!//SoundBuffer(file)
    val source = SoundSource(true, true)
    source.setPosition(Vector3f(0f, 0f, 0f))
    source.setVelocity(Vector3f(0f, 0f, 0f))
    source.setBuffer(buffer.buffer)

    println("start")

    source.play()
}