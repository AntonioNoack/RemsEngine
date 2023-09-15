package me.anno.tests.mesh

import me.anno.engine.ui.PlaneShapes.circleBuffer
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    testSceneWithUI("CircleBuffer", circleBuffer[15 to false])
}