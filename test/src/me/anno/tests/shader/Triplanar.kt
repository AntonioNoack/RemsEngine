package me.anno.tests.shader

import me.anno.ecs.components.shaders.TriplanarMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXBase
import me.anno.utils.OS.pictures

fun main() {
    GFXBase.forceLoadRenderDoc()
    val mat = TriplanarMaterial()
    mat.diffuseMap = pictures.getChild("uv-checker.jpg")
    mat.normalMap = pictures.getChild("BricksNormal.png")
    testSceneWithUI("Triplanar Material", mat)
}