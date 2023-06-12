package me.anno.tests.shader

import me.anno.ecs.components.shaders.AutoTileableMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.erf
import me.anno.maths.Maths.erfInv
import me.anno.utils.OS.pictures

fun main() {
    /*for (i in 0 until 100) {
        val r = Math.random().toFloat()
        println(erfInv(erf(r)) / r - 1f)
    }*/
    val material = AutoTileableMaterial()
    material.diffuseMap = pictures.getChild("textures/grass.jpg")
    testSceneWithUI(material)
}