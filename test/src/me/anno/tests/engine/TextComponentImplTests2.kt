package me.anno.tests.engine

import me.anno.ecs.components.text.SDFTextComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    OfficialExtensions.initForTests()
    testSceneWithUI("TextIn3D", TextComponentImplTests().createScene(SDFTextComponent()))
}
