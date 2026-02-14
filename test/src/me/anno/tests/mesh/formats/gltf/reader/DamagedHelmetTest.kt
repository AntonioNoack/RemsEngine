package me.anno.tests.mesh.gltf.reader

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

// todo bug: helmet is missing shine... why??? metallic/roughness??
fun main() {
    OfficialExtensions.initForTests()
    val file = downloads.getChild("3d/DamagedHelmet.glb")
    testSceneWithUI(file.nameWithoutExtension, file)
}