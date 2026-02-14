package me.anno.tests.mesh.shapes

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference

fun main() {
    // original bug: meshes were missing...
    // are they available in normal engine run mode??? -> yes
    OfficialExtensions.initForTests()
    val scene = Entity()
    scene.add(MeshComponent(getReference("meshes/CylinderY.json")))
    testSceneWithUI("Default Mesh", scene)
}