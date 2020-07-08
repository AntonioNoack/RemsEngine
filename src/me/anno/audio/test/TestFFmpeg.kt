package me.anno.audio.test

import me.anno.video.FFMPEGStream
import java.io.File

fun main2(){
    val frequency = 44100
    val file = File("C:\\Users\\Antonio\\Videos\\Captures\\bugs\\Watch_Dogs 2 2019-11-24 18-17-49.mp4")
    FFMPEGStream.getAudioSequence(file, 20f, 10f, frequency)
}