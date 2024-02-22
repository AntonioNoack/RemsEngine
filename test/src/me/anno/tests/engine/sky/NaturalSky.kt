package me.anno.tests.engine.sky

import me.anno.ecs.components.light.sky.Skybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    testSceneWithUI("Natural Sky", Skybox())
}