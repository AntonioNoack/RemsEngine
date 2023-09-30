package me.anno.tests.ecs

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.tests.gfx.metalRoughness

fun main() {
    testSceneWithUI("Metallic Reflections", metalRoughness())
}