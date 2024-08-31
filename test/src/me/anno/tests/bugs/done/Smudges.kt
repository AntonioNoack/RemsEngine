package me.anno.tests.bugs.done

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

/**
 * fixed: the forward-reflections were wayyy too light compared to deferred rendering
 * smudges should look the same in forward and deferred
 *  -> only possible, if we switch back to separate metallic and roughness
 * todo and forward reflections are now waaaayyy too bright, and shadows are too faint
 * */
fun main() {
    val src = downloads.getChild("3d/DamagedHelmet.glb")
    testSceneWithUI("Too Faint Smudges", src) {
        it.renderView.renderMode = RenderMode.FORWARD
    }
}