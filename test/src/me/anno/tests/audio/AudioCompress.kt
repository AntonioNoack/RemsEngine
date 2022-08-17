package me.anno.tests.audio

import me.anno.audio.openal.SoundBuffer
import me.anno.utils.OS.music
import me.anno.utils.files.Files.formatFileSize
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.DeflaterOutputStream

// test how easy it is to compress audio
fun main() {

    val source = music.getChild("test.ogg")

    // first step: load audio; -> 77% size reduction
    // then pack raw data into zip; -> 7% size reduction
    // then try with delta-encoding; -> 8.5% size reduction
    // -> no, existing formats are indeed useful

    val audio = SoundBuffer()
    audio.load0(source)

    val buffer = audio.data!!
    val halfSize = buffer.remaining() / 2
    val array = ShortArray(halfSize * 2) {
        if (it == 0) {
            buffer[0]
        } else {
            (buffer[it] - buffer[it - 1]).toShort()
        }
        // buffer[it]
    }

    val bos = ByteArrayOutputStream(4096)
    val dos = DataOutputStream(DeflaterOutputStream(bos))
    for (s in array)
        dos.writeShort(s.toInt())
    dos.close()

    println("" +
                "ogg: ${source.length().formatFileSize()}, " +
                "raw: ${(buffer.remaining() * 2L).formatFileSize()}, " +
                "zip: ${bos.size().toLong().formatFileSize()}"
    )

}