package me.anno.tests.shader

import me.anno.ecs.components.physics.FlagMesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

// generate cloth mesh
// simulate cloth using gfx shader
// render cloth
// apply forces like wind and gravity
fun main() {
    val flag = FlagMesh()
    flag.material.diffuseMap = getReference("res://textures/UVChecker.png")
    testSceneWithUI("FlagMeshSim", flag)
}