package me.anno.tests.image

import me.anno.config.DefaultConfig
import me.anno.ui.base.image.VideoPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.videos

fun main() {
    testUI3("Video Playback") {
        DefaultConfig["debug.renderdoc.enabled"] = true
        // solved bug: open video using image viewer -> random offset -> where coming from???, plus some flickering :/
        //  - only happens until we jump, so is the first reading attempt borked???
        // done why is playback limited to ~30 fps?
        // to do why is playback limited in some sections to 50 fps?
        // texImage2D() seems to be the culprit...
        val files = videos.listChildren()
            .first { !it.isDirectory }
        VideoPanel.createSimpleVideoPlayer(files)
    }
}