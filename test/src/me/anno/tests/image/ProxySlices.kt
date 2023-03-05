package me.anno.tests.image

import me.anno.Engine
import me.anno.utils.OS.videos
import me.anno.video.VideoProxyCreator

fun main() {
    // test proxy generation with slices
    val src = videos.getChild("2023-02-28 09-24-10.mkv")
    for (i in 0 until 100L) {
        println(VideoProxyCreator.getProxyFile(src, i, false) ?: break)
    }
    Engine.requestShutdown()
}