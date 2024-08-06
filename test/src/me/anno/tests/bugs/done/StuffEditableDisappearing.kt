package me.anno.tests.bugs.done

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube

class NonRegisteredClass: Component()

fun main() {
    val scene = Entity("Scene")
    scene.add(MeshComponent(flatCube.front))
    // open this, and change its name -> tons of error messages before our fix
    scene.add(NonRegisteredClass())
    testSceneWithUI("Disappearing", scene)
}