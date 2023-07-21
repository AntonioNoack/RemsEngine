package me.anno.tests.image

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.videos

fun main() {
    testUI3("Video Playback") {
        // done why is playback limited to ~30 fps?
        // to do why is playback limited in some sections to 50 fps?
        // texImage2D() seems to be the culprit...
        // todo -> create a video streaming service, where video can be extracted as a single, long stream,
        //  with less performance and management headaches
        FileExplorer(videos, style)
    }
}