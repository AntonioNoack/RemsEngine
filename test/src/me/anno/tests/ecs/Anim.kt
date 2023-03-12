package me.anno.tests.ecs

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.ECSFileExplorer
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.OS.downloads

fun main() {
    testUI3 {
        ECSRegistry.init()
        ECSFileExplorer(downloads.getChild("3d/FemaleStandingPose"), style)
    }
}