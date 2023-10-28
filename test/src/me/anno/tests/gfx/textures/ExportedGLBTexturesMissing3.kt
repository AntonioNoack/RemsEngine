package me.anno.tests.gfx.textures

import me.anno.Build
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    ECSRegistry.init()
    Build.isDebug = false
    LogManager.setLevel(null, Level.ALL)
    testSceneWithUI(".blend with materials", downloads.getChild("The Junk Shop.blend"))
}
