package me.anno.tests.engine.sky

import me.anno.ecs.components.shaders.CubemapSkybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    val skybox = CubemapSkybox()
    skybox.imageFile = downloads.getChild("2d/cross cubemap.jpg")
    testSceneWithUI("CubemapSkybox", skybox)
}