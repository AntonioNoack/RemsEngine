package me.anno.tests.ecs

import me.anno.config.DefaultConfig.style
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.ui.editor.files.FileExplorer
import me.anno.utils.OS.downloads

fun main() {
    testUI3 {
        FileExplorer(downloads.getChild("3d/FemaleStandingPose"), style)
    }
}