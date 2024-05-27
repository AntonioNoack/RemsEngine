package me.anno.tests.engine

import me.anno.ecs.components.light.CircleLight
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    testSceneWithUI("Light for Testing", CircleLight())
}