package me.anno.tests.mesh

import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.initMeshes()
    testSceneWithUI("Classroom", getReference("C:/Users/Antonio/Downloads/3d/ClassRoom/classroom.glb"))
}