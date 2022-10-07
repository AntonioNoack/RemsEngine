package me.anno.tests.ecs

import me.anno.ecs.components.shaders.AutoTileableMaterial
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.GFXBase
import me.anno.maths.Maths.erf
import me.anno.maths.Maths.erfInv
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.OS.pictures

fun main() {

    for (i in 0 until 100) {
        val r = Math.random().toFloat()
        println(erfInv(erf(r)) / r - 1f)
    }

    // test for auto-tileable material
    GFXBase.forceLoadRenderDoc()
    testUI {
        val material = AutoTileableMaterial()
        material.diffuseMap = pictures.getChild("textures/grass.jpg")
        testScene(material)
    }

}