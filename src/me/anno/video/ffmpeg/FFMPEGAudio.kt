package me.anno.video.ffmpeg

import me.anno.Engine
import me.anno.audio.WaveReader
import me.anno.audio.openal.SoundBuffer
import me.anno.io.files.FileReference
import me.anno.utils.ShutdownException
import org.lwjgl.openal.AL10.AL_FORMAT_MONO16
import org.lwjgl.openal.AL10.AL_FORMAT_STEREO16
import kotlin.concurrent.thread

class FFMPEGAudio(file: FileReference?, val sampleRate: Int, val length: Double) :
    FFMPEGStream(file, false) {
    // audio should be fast -> not limited

    override fun process(process: Process, vararg arguments: String) {
        // ("starting process for audio $sampleRate x $length")
        // (arguments)
        thread(name = "${file?.name}:error-stream") {
            val out = process.errorStream.bufferedReader()
            val parser = FFMPEGMetaParser()
            try {
                while (!Engine.shutdown) {
                    val line = out.readLine() ?: break
                    parser.parseLine(line, this)
                }
            } catch (e: ShutdownException) {
                // ...
            }
            out.close()
        }
        process.inputStream.buffered().use { input ->
            val frameCount = (sampleRate * length).toInt()
            input.mark(3)
            if (input.read() < 0) { // EOF
                isEmpty = true
                return//@thread
            }
            input.reset()
            val buffer = SoundBuffer()
            try {
                val (bytes, shorts, stereo) = WaveReader.readWAV(input, frameCount)
                buffer.loadRaw16(
                    shorts, bytes, sampleRate,
                    if (stereo) AL_FORMAT_STEREO16 else AL_FORMAT_MONO16
                )
                soundBuffer = buffer
            } catch (ignored: ShutdownException) {
            }
        }
    }

    var isEmpty = false
    var soundBuffer: SoundBuffer? = null

    override fun destroy() {}

}