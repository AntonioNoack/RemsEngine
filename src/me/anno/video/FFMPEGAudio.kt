package me.anno.video

import me.anno.audio.openal.SoundBuffer
import me.anno.audio.format.WaveReader
import me.anno.io.FileReference
import me.anno.utils.ShutdownException
import kotlin.concurrent.thread

class FFMPEGAudio(file: FileReference?, val sampleRate: Int, val length: Double) :
    FFMPEGStream(file, false) {
    // audio should be fast -> not limited

    override fun process(process: Process, arguments: List<String>) {
        // ("starting process for audio $sampleRate x $length")
        // (arguments)
        thread(name = "${file?.name}:error-stream") {
            val out = process.errorStream.bufferedReader()
            val parser = FFMPEGMetaParser()
            try {
                while (true) {
                    val line = out.readLine() ?: break
                    parser.parseLine(line, this)
                }
            } catch (e: ShutdownException){
                // ...
            }
            out.close()
        }
        thread(name = "${file?.name}:input-stream") {
            val input = process.inputStream.buffered()
            val frameCount = (sampleRate * length).toInt()
            input.mark(3)
            if (input.read() < 0) { // EOF
                isEmpty = true
                return@thread
            }
            input.reset()
            try {
                val wav = WaveReader(input, frameCount)
                val buffer = SoundBuffer()
                buffer.loadRawStereo16(wav.stereoPCM, sampleRate)
                soundBuffer = buffer
            } catch (e: ShutdownException){
                // ...
            }
            input.close()
        }
    }

    var isEmpty = false
    var soundBuffer: SoundBuffer? = null

    override fun destroy() {}

}