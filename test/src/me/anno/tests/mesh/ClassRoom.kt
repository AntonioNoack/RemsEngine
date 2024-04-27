package me.anno.tests.mesh

import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.initMeshes()
    testSceneWithUI("Classroom", downloads.getChild("3d/ClassRoom/classroom.glb"))
}