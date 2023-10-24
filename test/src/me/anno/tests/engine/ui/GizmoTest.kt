package me.anno.tests.engine.ui

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.Shapes.flatCube

fun main() {
    testSceneWithUI("Gizmos", Entity(MeshComponent(flatCube.front)))
}