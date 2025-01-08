package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val scene = Entity()
    val s = 10
    val v = 2.1
    for (z in -s..s) {
        for (y in -s..s) {
            for (x in -s..s) {
                val comp = MeshComponent(flatCube)
                comp.isInstanced = true
                Entity(scene)
                    .setPosition(x * v, y * v, z * v)
                    .add(comp)
            }
        }
    }
    testSceneWithUI("Ortho Frustum", scene) {
        it.renderView.editorCamera.isPerspective = false
    }
}