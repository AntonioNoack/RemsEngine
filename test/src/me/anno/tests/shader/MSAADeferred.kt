package me.anno.tests.shader

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    testSceneWithUI(downloads.getChild("3d/DamagedHelmet.glb")) {
        it.renderer.renderMode = RenderMode.MSAA_DEFERRED
    }
}