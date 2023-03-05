package me.anno.tests.image

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.videos

fun main() {
    // todo test proxy generation speed
    // todo also test speed on how quickly FFMPEG is called, thumb generation seems rather slow
    testUI3 {
        FileExplorer(videos, style)
    }
}