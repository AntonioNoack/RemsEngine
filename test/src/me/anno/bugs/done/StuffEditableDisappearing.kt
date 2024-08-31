package me.anno.bugs.done

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube

class NonRegisteredClass(val title: String): Component()

fun main() {
    val scene = Entity("Scene")
    scene.add(MeshComponent(flatCube.front))
    // open this, and change its name -> tons of error messages before our fix
    // todo this still throws tons of error messages, if no default constructor is available
    scene.add(NonRegisteredClass("Test"))
    testSceneWithUI("Disappearing", scene)
}