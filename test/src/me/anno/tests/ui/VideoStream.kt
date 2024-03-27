package me.anno.tests.ui

import me.anno.engine.OfficialExtensions
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.ui.base.image.VideoPanel.Companion.createSimpleVideoPlayer
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.videos

fun main() {
    OfficialExtensions.initForTests()
    disableRenderDoc()
    testUI3("Video Playback") {
        val source = videos.getChild("treemiddle.mp4")
        createSimpleVideoPlayer(source).fill(1f)
    }
}