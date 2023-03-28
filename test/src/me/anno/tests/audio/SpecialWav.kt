package me.anno.tests.audio

import me.anno.io.files.FileReference.Companion.getReference
import javax.sound.sampled.AudioSystem


fun main() {

    // the Sonniss.com - GDC 2023 - Game Audio Bundle contains .wav files with format=65534, so a custom format
    // we shouldn't implement it except if it is used a lot -> use preexisting methods
    // -> use FFMPEG, and let it return raw audio :D
    val ref = getReference(
        "E:/Assets/Torrent/Sonniss.com - GDC 2023 - Game Audio Bundle/344 Audio - Commercial Aircraft Interior/" +
                "AEROJet_INT Aeroplane Flight Landing Announcement,_344 Audio_Commercial Aircraft Interior.wav"
    )

    var totalFramesRead = 0
    ref.inputStreamSync().use {
        val audioInputStream = AudioSystem.getAudioInputStream(it)
        println(audioInputStream.format)
        val bytesPerFrame = audioInputStream.format.frameSize

        // Set an arbitrary buffer size of 1024 frames.
        val numBytes = 1024 * bytesPerFrame
        val audioBytes = ByteArray(numBytes)

        // Try to read numBytes bytes from the file.
        while (true) {
            val numBytesRead = audioInputStream.read(audioBytes)
            if (numBytesRead < 0) break
            val numFramesRead = numBytesRead / bytesPerFrame
            totalFramesRead += numFramesRead

        }

        println("read $totalFramesRead frames total")
    }

}