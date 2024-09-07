package me.anno.tests.mesh.gltf

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    testSceneWithUI("Classroom", downloads.getChild("3d/ClassRoom/classroom.glb"))
}