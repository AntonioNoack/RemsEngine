package me.anno.tests.mesh.blender

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestStudio.Companion.testUI3

fun main() {
    testUI3("Blender Thumbnails") {
        val src = getReference("E:/Documents/Blender")
        val panel = CustomList(false, style)
        panel.add(ECSFileExplorer(src, style))
        panel.add(SceneView(PlayMode.EDITING, style))
        panel
    }
}