package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val scene = Entity("Scene")
    for (i in 0 until 5) {
        val mesh = IcosahedronModel.createIcosphere(i)
        val child = Entity("LOD $i", scene)
        child.setPosition(i * 2.5, 0.0, 0.0)
        child.add(MeshComponent(mesh))
    }
    testSceneWithUI("Icosphere", scene)
}