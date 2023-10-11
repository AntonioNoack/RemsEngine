package me.anno.tests.ecs

import me.anno.ecs.components.shaders.UVSkybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    val skybox = UVSkybox()
    skybox.imageFile = downloads.getChild("2d/qwantani_1k.hdr")
    testSceneWithUI("UVSkybox", skybox)
}