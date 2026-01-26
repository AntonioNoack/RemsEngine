package me.anno.tests.recast

import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.sq
import me.anno.tests.recast.FlatRecastTest.Companion.calculateTotalArea
import me.anno.tests.recast.FlatRecastTest.Companion.createRecastScene
import me.anno.tests.recast.FlatRecastTest.Companion.scale
import me.anno.utils.types.Floats.formatPercent

fun main() {
    val (scene, navMeshData) = createRecastScene(DefaultAssets.plane)
    val expectedArea = sq(scale * 2f)
    val actualArea = calculateTotalArea(navMeshData.meshData)
    println("Found area: ${(actualArea / expectedArea).formatPercent()}")
    testSceneWithUI("Incorrect Recast Mesh", scene)
}