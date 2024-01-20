package me.anno.io.files.inner.temporary

import me.anno.audio.AudioReadable
import java.io.InputStream

abstract class InnerTmpAudioFile : InnerTmpFile("mp3"), AudioReadable {
    override fun inputStreamSync(): InputStream = throw NotImplementedError()
}