package me.anno.tests.sdf

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.sdf.shapes.SDFSphere

fun main() {
    registerCustomClass(SDFSphere())
    testSceneWithUI("SDFSphere", SDFSphere())
}