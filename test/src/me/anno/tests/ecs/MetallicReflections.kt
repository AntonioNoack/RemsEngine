package me.anno.tests.ecs

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents

fun main() {
    testSceneWithUI("Metallic Reflections", documents.getChild("metal-roughness.glb"))
}