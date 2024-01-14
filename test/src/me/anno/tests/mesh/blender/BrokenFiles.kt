package me.anno.tests.mesh.blender

import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.initMeshes()
    testSceneWithUI("Broken Blender Files", getReference("E:/Documents/Blender/AnalyseGM.blend"))
}