package me.anno.tests.engine

import me.anno.ecs.components.light.CircleLight
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // todo we should add a plane/setup below (in RenderView), so the light is visible
    testSceneWithUI("Light for Testing", CircleLight())
}