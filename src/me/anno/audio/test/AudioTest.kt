package me.anno.audio.test

import me.anno.audio.openal.*
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.studio.StudioBase
import me.anno.utils.OS
import me.anno.video.FFMPEGStream
import org.joml.Vector3f

fun main() {

    AudioManager.init()

    try {

        SoundListener.setPosition(Vector3f())
        SoundListener.setVelocity(Vector3f())
        SoundListener.setOrientation(Vector3f(0f, 0f, 1f), Vector3f(0f, 1f, 0f))

        testStream()
        while (true) {
            AudioTasks.workQueue()
        }
        // testSingleBuffer()

        /*val time0 = System.nanoTime()
        while(true){
            val dt = (System.nanoTime() - time0)*1e-9f
            if(dt > 30) break

            val arc = dt
            val rad = 100f
            SoundListener.setPosition(Vector3f(sin(arc) * rad, 0f, cos(arc) * rad))
            // source.setGain(sin(arc)*0.5f+0.5f)

            sleepShortly()
        }*/

        // source.destroy()

        // buffer.destroy()

    } finally {
        StudioBase.shallStop = true
    }

}

val file = getReference(OS.videos, "Captures\\cool cops\\Watch_Dogs 2 2019-10-14 15-26-49.mp4")

fun testStream() {
    // AudioStreamOpenAL(file, LoopingState.PLAY_LOOP, 0.0, FFMPEGMetadata(file), Camera()).start()
}

fun testSingleBuffer() {
    val buffer2 = FFMPEGStream.getAudioSequence(file, 0.0, 10.0, 48000)
    var buffer2a: SoundBuffer?
    while (true) {
        buffer2a = buffer2.soundBuffer
        if (buffer2a != null) break
        // work on the queue
        AudioTasks.workQueue()
    }

    val buffer = buffer2a!!//SoundBuffer(file)
    val source = SoundSource(true, true)
    source.setPosition(Vector3f(0f, 0f, 0f))
    source.setVelocity(Vector3f(0f, 0f, 0f))
    source.setBuffer(buffer.pointer)

    source.play()
}