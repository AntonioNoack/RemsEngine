package me.anno.tests.engine.material

import me.anno.ecs.components.shaders.PlanarMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.pictures

/**
 * PlanarMaterial: uvs are projected using world coordinates
 * */
fun main() {
    val mat = PlanarMaterial()
    mat.diffuseMap = pictures.getChild("BricksColor.jpg")
    mat.normalMap = pictures.getChild("BricksNormal.png")
    testSceneWithUI("Planar Material", mat)
}