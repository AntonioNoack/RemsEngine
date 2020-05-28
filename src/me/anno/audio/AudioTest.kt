package me.anno.audio

import org.joml.Vector3f
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

fun main(){
    AudioManager.init()

    try {

        val listener = SoundListener
        listener.setPosition(Vector3f())
        listener.setVelocity(Vector3f())
        listener.setOrientation(Vector3f(0f, 0f, 1f), Vector3f(0f, 1f, 0f))

        val buffer = SoundBuffer(File("C:\\Users\\Antonio\\Music\\test.ogg"))
        val source = SoundSource(true, true)
        source.setPosition(Vector3f(0f, 0f, 0f))
        source.setVelocity(Vector3f(0f, 0f, 0f))
        source.setBuffer(buffer.buffer)

        source.play()

        val time0 = System.nanoTime()
        while(true){
            val dt = (System.nanoTime() - time0)*1e-9f
            if(dt > 30) break

            val arc = dt
            val rad = 100f
            listener.setPosition(Vector3f(sin(arc)*rad, 0f, cos(arc)*rad))
            source.setGain(sin(arc)*0.5f+0.5f)

            Thread.sleep(10)
        }


        source.destroy()

        buffer.destroy()

    } finally {
        AudioManager.destroy()
    }

}