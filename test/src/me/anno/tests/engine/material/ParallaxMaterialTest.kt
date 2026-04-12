package me.anno.tests.engine.material

import me.anno.ecs.components.mesh.material.ParallaxMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.pictures

fun main() {
    val folder = pictures.getChild("Textures/marble_cliff_01_4k.blend/textures")
    val material = ParallaxMaterial().apply {
        diffuseMap = folder.getChild("marble_cliff_01_diff_4k.jpg")
        normalMap = folder.getChild("marble_cliff_01_nor_gl_4k.exr")
        heightMap = folder.getChild("marble_cliff_01_disp_4k.png")
        roughnessMap = folder.getChild("marble_cliff_01_rough_4k.exr")
        roughnessMinMax.set(0f, 1f)
    }
    testSceneWithUI("Parallax Test", material)
}