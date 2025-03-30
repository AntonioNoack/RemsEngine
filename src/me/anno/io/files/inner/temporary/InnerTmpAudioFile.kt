package me.anno.io.files.inner.temporary

import me.anno.audio.AudioReadable
import me.anno.utils.async.Callback
import java.io.IOException
import java.io.InputStream

abstract class InnerTmpAudioFile : InnerTmpFile("mp3"), AudioReadable {
    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.err(IOException("Audio encoding not yet implemented"))
    }

    override fun length(): Long {
        return 100_000L // just a guess
    }
}