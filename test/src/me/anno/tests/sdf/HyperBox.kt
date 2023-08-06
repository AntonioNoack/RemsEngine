package me.anno.tests.sdf

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.shapes.SDFHyperBBox

fun main() {
    testSceneWithUI("SDFHyperBox", SDFHyperBBox())
}