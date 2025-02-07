package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    // todo the engine is awfully slow... we should be able to handle more than 68k instances
    //  ( rendering takes 11ms, but together only 20 fps :( )
    val scene = Entity("Scene")
    val s = 20
    val p = 2.1
    val mesh = IcosahedronModel.createIcosphere(4)
    for (z in -s..s) {
        val ze = Entity(scene)
        for (y in -s..s) {
            for (x in -s..s) {
                Entity(ze)
                    .add(MeshComponent(mesh))
                    .setPosition(x * p, y * p, z * p)
                    .setScale(1.0, 1.0, 1.0)
            }
        }
    }
    testSceneWithUI("BoxCulling", scene)
}