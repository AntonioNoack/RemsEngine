package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.TAUf
import me.anno.sdf.shapes.SDFBox

fun createShapesScene2(limit: Int): Entity {
    val scene = Entity()
    for (i in 0 until limit) {
        val angle = i * TAUf / limit
        val shape = SDFBox()
        shape.position.set(0f, 0f, 5f).rotateY(angle)
        scene.add(shape)
    }
    return scene
}

fun main() {
    OfficialExtensions.initForTests()
    testSceneWithUI("SDF Shapes", createShapesScene2(20))
}