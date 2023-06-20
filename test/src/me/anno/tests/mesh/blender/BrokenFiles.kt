package me.anno.tests.mesh.blender

import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    ECSRegistry.initMeshes()
    testSceneWithUI(getReference("E:/Documents/Blender/AnalyseGM.blend"))
}