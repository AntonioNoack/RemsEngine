package me.anno.video

import me.anno.audio.ALBase
import me.anno.audio.SoundBuffer
import me.anno.gpu.GFX
import org.newdawn.slick.openal.WaveData
import java.io.File
import kotlin.concurrent.thread

class FFMPEGAudio(file: File?, val sampleRate: Int, val length: Float, val frame0: Int):
    FFMPEGStream(file){

    override fun process(process: Process, arguments: List<String>) {
        thread {
            val out = process.errorStream.bufferedReader()
            val parser = FFMPEGMetaParser()
            while(true){
                val line = out.readLine() ?: break
                parser.parseLine(line, this)
            }
        }
        thread {
            val input = process.inputStream.buffered()
            val frameCount = (sampleRate * length).toInt()
            val wav = WaveData.create(input, frameCount)
            GFX.addAudioTask {
                val buffer = SoundBuffer(wav)
                soundBuffer = buffer
                ALBase.check()
                10
            }
            input.close()
        }
    }

    var soundBuffer: SoundBuffer? = null

    override fun destroy() {}

}