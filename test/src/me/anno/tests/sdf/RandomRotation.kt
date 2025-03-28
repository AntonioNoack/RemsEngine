package me.anno.tests.sdf

import me.anno.engine.DefaultAssets
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.sdf.arrays.SDFArrayMapper
import me.anno.sdf.random.SDFRandomRotation
import me.anno.sdf.shapes.SDFBox

fun main() {
    OfficialExtensions.initForTests()

    val material = DefaultAssets.goldenMaterial

    val array = SDFArrayMapper()
    array.cellSize.set(2f)
    array.count.set(10, 1, 10)

    val rot = SDFRandomRotation()
    rot.minAngleDegrees.set(-2f, 0f, -2f)
    rot.maxAngleDegrees.set(+2f, 0f, +2f)

    val shape = SDFBox()
    shape.sdfMaterials = listOf(material.ref)
    shape.addChild(array)
    shape.addChild(rot)

    disableRenderDoc()
    testSceneWithUI("SDF Random Rotation", shape)
}