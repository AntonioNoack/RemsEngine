package me.anno.tests.ecs

import me.anno.ecs.components.shaders.AutoTileableMaterial
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.GFX
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.pictures

fun main() {
    // test for auto-tileable material
    GFX.forceLoadRenderDoc()
    testUI {
        val material = AutoTileableMaterial()
        material.diffuseMap = pictures.getChild("textures/grass.jpg")
        testScene(material)
    }
}