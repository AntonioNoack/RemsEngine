package me.anno.tests.engine

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.LODMeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val s = 5
    val spacing = 2.4
    val scene = Entity("Scene")
    val meshes = (3 downTo 0).map { subDivisions ->
        IcosahedronModel.createIcosphere(subDivisions).ref
    }
    for (zi in -s..s) {
        val rowZ = Entity("Z:$zi", scene)
            .setPosition(0.0, 0.0, spacing * zi)
        for (yi in -s..s) {
            val rowY = Entity("Y:$yi", rowZ)
                .setPosition(0.0, spacing * yi, 0.0)
            for (x in -s..s) {
                val mesh = LODMeshComponent()
                mesh.meshes = meshes
                Entity("X:$x", rowY)
                    .setPosition(x * spacing, 0.0, 0.0)
                    .add(mesh)
            }
        }
    }
    testSceneWithUI("LodMeshes", scene)
}