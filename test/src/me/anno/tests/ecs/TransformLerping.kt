package me.anno.tests.ecs

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes
import me.anno.utils.types.Floats.toRadians

fun main() {
    val scene = Entity()
    val child = Entity()
    child.add(MeshComponent(Shapes.flatCube.front.ref))
    child.add(object : Component() {
        override fun onUpdate(): Int {
            transform!!.rotateYLocal(120.0.toRadians())
            return 40
        }
    })
    scene.add(child)
    testSceneWithUI(scene)
}