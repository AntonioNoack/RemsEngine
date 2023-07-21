package me.anno.tests.mesh

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.utils.OS.documents

fun main() {
    // not a single of my files can be loaded with Assimp 4.1 ...
    // BLEND: Expected at least one object with no parent
    val file = documents.getChild("Blender").listChildren()!!
    for (child in file) {
        if (child.lcExtension == "blend")
            try {
                AnimatedMeshesLoader.readAsFolder(child)
                testSceneWithUI("Assimp/Blender", child)
                return
            } catch (ignored: Exception) {

            }
    }
}