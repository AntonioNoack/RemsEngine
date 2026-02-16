package me.anno.tests.engine.effect

import me.anno.ecs.Entity
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.visual.render.effects.HeightExpFogSettings
import me.anno.tests.utils.TestWorld

fun main() {
    val scene = Entity("Scene")
    val settings = HeightExpFogSettings()
    scene.add(TestWorld().createTriangleMesh(0, 0, 0, 256, 32, 512))
    scene.add(settings)
    testSceneWithUI("HeightFog", scene, RenderMode.FOG_TEST) {
        EditorState.select(settings)
    }
}