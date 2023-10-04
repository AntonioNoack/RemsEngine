package me.anno.tests.gfx

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes

fun main() {
    val scene = Entity()
    val meshFile = Shapes.flatCube.front.ref
    val s = 10
    val v = 2.1
    for (z in -s..s) {
        for (y in -s..s) {
            for (x in -s..s) {
                val mesh = Entity()
                mesh.setPosition(x * v, y * v, z * v)
                val comp = MeshComponent(meshFile)
                comp.isInstanced = true
                mesh.add(comp)
                scene.add(mesh)
            }
        }
    }
    testSceneWithUI("Ortho Frustum", scene) {
        it.renderer.editorCamera.isPerspective = false
    }
}