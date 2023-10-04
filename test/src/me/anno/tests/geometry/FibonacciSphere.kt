package me.anno.tests.geometry

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.geometry.FibonacciSphere
import me.anno.mesh.Shapes.smoothCube

fun main() {
    val scene = Entity()
    for (pt in FibonacciSphere.create(12)) {
        val child = Entity()
        child.position = child.position.set(pt)
        child.setScale(0.03)
        child.add(MeshComponent(smoothCube.front))
        scene.add(child)
    }
    testSceneWithUI("FibonacciSphere", scene)
}