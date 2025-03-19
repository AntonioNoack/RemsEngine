package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.sdf.shapes.SDFBox

fun createShapesScene2(numBlocks: Int): Entity {
    val scene = Entity()
    val radius = numBlocks / PIf - 1f
    for (i in 0 until numBlocks) {
        val angle = i * TAUf / numBlocks
        val y = (i and 1) * 0.05f
        val shape = SDFBox()
        shape.name = "Box[$i]"
        shape.position.set(0f, y, radius).rotateY(angle)
        shape.rotation.rotationY(angle)
        scene.add(shape)
    }
    return scene
}

fun main() {
    OfficialExtensions.initForTests()
    testSceneWithUI("SDF Shapes", createShapesScene2(20))
}