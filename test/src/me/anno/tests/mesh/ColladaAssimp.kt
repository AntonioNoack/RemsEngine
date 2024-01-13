package me.anno.tests.mesh

import me.anno.engine.PluginRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.utils.OS

fun main() {
    PluginRegistry.init()
    ExtensionLoader.load()
    // not a single of my files can be loaded with Assimp 4.1 ...
    // BLEND: Expected at least one object with no parent
    val file = OS.downloads.getChild("3d/FemaleStandingPose").listChildren()!!
    for (child in file) {
        if (child.lcExtension == "dae")
            try {
                AnimatedMeshesLoader.readAsFolder(child)
                testSceneWithUI("Assimp/Collada", child)
                return
            } catch (ignored: Exception) {
                ignored.printStackTrace()
            }
    }
}