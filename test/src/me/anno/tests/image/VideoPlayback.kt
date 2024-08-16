package me.anno.tests.image

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.videos

fun main() {
    testUI3("Video Playback") {
        // todo new bug: open video using image viewer -> random offset -> where coming from???, plus some flickering :/
        //  - only happens until we jump, so is the first reading attempt borked???
        // done why is playback limited to ~30 fps?
        // to do why is playback limited in some sections to 50 fps?
        // texImage2D() seems to be the culprit...
        FileExplorer(videos, true, style)
    }
}