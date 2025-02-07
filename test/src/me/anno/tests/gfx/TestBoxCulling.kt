package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // todo the engine is awfully slow... we should be able to handle more than 68k instances
    //  ( rendering takes 11ms, but together only 28 fps :( )
    val mesh = IcosahedronModel.createIcosphere(4)
    testSceneWithUI("BoxCulling", createMeshBox(mesh, 20))
}

/**
 * creates a (2s+1)³ cube of meshes
 * */
fun createMeshBox(mesh: Mesh, s: Int): Entity {
    val spacing = 2.1
    val scene = Entity("Scene")
    for (z in -s..s) {
        val ze = Entity(scene)
        for (y in -s..s) {
            val ye = Entity(ze)
            for (x in -s..s) {
                Entity(ye)
                    .add(MeshComponent(mesh))
                    .setPosition(x * spacing, y * spacing, z * spacing)
                    .setScale(1.0, 1.0, 1.0)
            }
        }
    }
    return scene
}