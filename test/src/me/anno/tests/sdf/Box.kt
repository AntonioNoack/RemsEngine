package me.anno.tests.sdf

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.shapes.SDFBox

fun main() {
    testSceneWithUI("SDFBox", SDFBox())
}