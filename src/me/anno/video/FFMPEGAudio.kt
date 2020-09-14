package me.anno.video

import me.anno.audio.ALBase
import me.anno.audio.SoundBuffer
import me.anno.gpu.GFX
import me.anno.audio.format.WaveReader
import java.io.File
import kotlin.concurrent.thread

class FFMPEGAudio(file: File?, val sampleRate: Int, val length: Double):
    FFMPEGStream(file){

    override fun process(process: Process, arguments: List<String>) {
        // println("starting process for audio $sampleRate x $length")
        // println(arguments)
        thread {
            val out = process.errorStream.bufferedReader()
            val parser = FFMPEGMetaParser()
            while(true){
                val line = out.readLine() ?: break
                // println("meta $line")
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
            GFX.addAudioTask(10){
                // println("got reader and is loading now...")
                val buffer = SoundBuffer()
                buffer.loadRawStereo16(wav.stereoPCM, sampleRate)
                soundBuffer = buffer
                ALBase.check()
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