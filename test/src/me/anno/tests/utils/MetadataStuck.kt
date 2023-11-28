package me.anno.tests.utils

import me.anno.Engine
import me.anno.utils.OS.videos
import me.anno.video.ffmpeg.MediaMetadata.Companion.getMeta
import org.apache.logging.log4j.LogManager

fun main() {
    // this wasn't terminating sometimes
    // probably the error stream was blocking the input stream...
    LogManager.logAll()
    val file = videos.getChild("2022-12-08 11-22-55.mkv")
    println(getMeta(file, false))
    Engine.requestShutdown()
}