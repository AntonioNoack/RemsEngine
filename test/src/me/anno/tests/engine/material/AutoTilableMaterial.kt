package me.anno.tests.engine.material

import me.anno.ecs.components.mesh.material.AutoTileableMaterial
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.pictures

fun main() {
    val material = AutoTileableMaterial()
    material.diffuseMap = pictures.getChild("textures/grass.jpg")
    testSceneWithUI("AutoTileable Material", material)
}