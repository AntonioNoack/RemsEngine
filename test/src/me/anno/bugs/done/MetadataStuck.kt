package me.anno.bugs.done

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.videos
import me.anno.io.MediaMetadata.Companion.getMeta
import org.apache.logging.log4j.LogManager

fun main() {
    // this wasn't terminating sometimes
    // probably the error stream was blocking the input stream...
    OfficialExtensions.initForTests()
    LogManager.logAll()
    val file = videos.getChild("2022-12-08 11-22-55.mkv")
    println(getMeta(file).waitFor())
    Engine.requestShutdown()
}