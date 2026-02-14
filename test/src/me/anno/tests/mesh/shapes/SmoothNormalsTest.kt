package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.utils.NormalCalculator.calculateSmoothNormals
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    val source = desktop.getChild("Plate48x48.json")
    val mesh = MeshCache.getEntry(source).waitFor() as Mesh
    calculateSmoothNormals(mesh, 0.9f, 0f)
    testSceneWithUI("Smooth Normals", mesh)
}