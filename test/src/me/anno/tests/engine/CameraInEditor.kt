package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.camera.Camera
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val camera = Camera()
    testSceneWithUI("Camera In UI", Entity().add(camera)) {
        EditorState.select(camera)
    }
}