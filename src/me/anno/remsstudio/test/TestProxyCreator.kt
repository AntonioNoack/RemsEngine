package me.anno.remsstudio.test

import me.anno.io.files.FileReference
import me.anno.remsstudio.RemsConfig
import me.anno.remsstudio.RemsStudio
import me.anno.utils.OS
import me.anno.video.VideoProxyCreator

fun main() {
    // test for a video file
    RemsStudio.setupNames()
    RemsConfig.init()
    VideoProxyCreator.getProxyFile(FileReference.getReference(OS.videos, "GodRays.mp4"))
}