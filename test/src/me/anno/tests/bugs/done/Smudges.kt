package me.anno.tests.bugs.done

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

/**
 * fixed: the forward-reflections were wayyy too light compared to deferred rendering
 * */
fun main() {
    val src = downloads.getChild("3d/DamagedHelmet.glb")
    testSceneWithUI("Too Faint Smudges", src) {
        it.renderer.renderMode = RenderMode.FORWARD
    }
}