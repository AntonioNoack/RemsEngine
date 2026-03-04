package me.anno.tests.shader

import me.anno.engine.Events.addEvent
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.ECSTreeView
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.OS.documents

fun main() {
    val scene = documents.getChild("RemsEngine/StuckUnderground/Rooms/Room3x4.json")
    // todo bug: f11 is not working inside input fields, and the scene view, why? scene view is especially important
    testSceneWithUI("Checkerboard", scene, RenderMode.CHECKERBOARD) { sv ->
        // try whether the remainder is the cause -> no :/
        addEvent {
            val ui = sv.window!!.panel
            ui.forAllPanels {
                when (it) {
                    is ECSTreeView,
                    is ECSFileExplorer,
                    is PropertyInspector -> it.isVisible = false
                }
            }
        }
    }
}