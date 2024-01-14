package me.anno.tests.mesh.blender

import me.anno.config.DefaultConfig.style
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestEngine.Companion.testUI3

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.init()
    testUI3("Blender Thumbnails") {
        val src = getReference("E:/Documents/Blender")
        val panel = CustomList(false, style)
        panel.add(ECSFileExplorer(src, style))
        panel.add(SceneView(PlayMode.EDITING, style))
        panel
    }
}