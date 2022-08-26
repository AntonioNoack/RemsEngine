package me.anno.tests.ecs

import me.anno.ecs.components.shaders.AutoTileableMaterial
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.maths.Maths.PIf
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.pictures

fun main() {
    // test for auto-tileable material
    testUI {
        val material = AutoTileableMaterial()
        // todo loading webp as image is broken :/
        material.diffuseMap = pictures.getChild("textures/grass.jpg")
        testScene(material)
    }
}