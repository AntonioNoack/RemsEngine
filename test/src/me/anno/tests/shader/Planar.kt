package me.anno.tests.shader

import me.anno.ecs.components.shaders.PlanarMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.pictures

fun main() {
    val mat = PlanarMaterial()
    mat.diffuseMap = pictures.getChild("BricksColor.jpg")
    mat.normalMap = pictures.getChild("BricksNormal.png")
    testSceneWithUI(mat)
}