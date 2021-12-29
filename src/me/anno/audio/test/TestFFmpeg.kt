package me.anno.audio.test

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS
import me.anno.video.FFMPEGStream

fun main2() {
    val frequency = 44100
    val file = getReference(OS.videos, "Captures\\bugs\\Watch_Dogs 2 2019-11-24 18-17-49.mp4")
    FFMPEGStream.getAudioSequence(file, 20.0, 10.0, frequency)
}