package me.anno.video

import me.anno.audio.SoundBuffer
import me.anno.gpu.GFX
import org.newdawn.slick.openal.WaveData
import java.io.File
import kotlin.concurrent.thread

class FFMPEGAudio(file: File?, val frame0: Int):
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
            val wav = WaveData.create(input, 48000 * 10)
            GFX.addAudioTask {
                val buffer = SoundBuffer(wav)
                soundBuffer = buffer
                10
            }
            input.close()
        }
    }

    var soundBuffer: SoundBuffer? = null

    override fun destroy() {}

}