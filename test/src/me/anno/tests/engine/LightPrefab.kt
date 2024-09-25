package me.anno.tests.engine

import me.anno.ecs.components.light.CircleLight
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // we should add a plane/setup below (in RenderView), so the light is visible
    // todo fog might be the most effective to show light...
    // todo diffuse rendering is missing metallic-reflection if roughness=0, forward is fine
    testSceneWithUI("Light for Testing", CircleLight())
}