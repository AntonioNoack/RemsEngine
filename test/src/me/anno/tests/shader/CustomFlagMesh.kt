package me.anno.tests.shader

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.CylinderModel
import me.anno.ecs.components.physics.FlagMesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.res

// todo custom mesh doesn't work :(
fun main() {
    val flag = FlagMesh()
    // flag.materials = listOf(Material().apply { diffuseMap = res.getChild("textures/UVChecker.png") }.ref)
    flag.material.diffuseMap = res.getChild("textures/UVChecker.png")
    flag.useCustomMesh = true
    flag.meshFile = CylinderModel.createCylinder(32, 2, true, true, null, 1f, Mesh()).ref
    testSceneWithUI("CustomFlagMesh", flag)
}