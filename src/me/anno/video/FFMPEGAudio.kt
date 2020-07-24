package me.anno.video

import me.anno.audio.ALBase
import me.anno.audio.SoundBuffer
import me.anno.gpu.GFX
import me.anno.audio.format.WaveReader
import java.io.File
import kotlin.concurrent.thread

class FFMPEGAudio(file: File?, val sampleRate: Int, val length: Double, val frame0: Int):
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
            input.mark(3)
            if(input.read() < 0){
                // EOF
                isEmpty = true
                return@thread
            }
            input.reset()
            val wav = WaveReader(input, frameCount)
            GFX.addAudioTask {
                val buffer = SoundBuffer()
                buffer.loadRawStereo16(wav.stereoPCM, sampleRate)
                soundBuffer = buffer
                ALBase.check()
                10
            }
            /*thread {
                // keep a reference to wav.stereoPCM, because we need it
                // it crashes the JVM in Java 8
                Thread.sleep(30_000)
                println(wav.stereoPCM.get(0))
            }*/
            /*val wav = WaveData.create(input, frameCount)
            if(wav != null){
                GFX.addAudioTask {
                    val buffer = SoundBuffer(wav)
                    soundBuffer = buffer
                    ALBase.check()
                    10
                }
            }*/
            input.close()
        }
    }

    var isEmpty = false
    var soundBuffer: SoundBuffer? = null

    override fun destroy() {}

}