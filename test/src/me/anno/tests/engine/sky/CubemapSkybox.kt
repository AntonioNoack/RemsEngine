package me.anno.tests.engine.sky

import me.anno.ecs.components.light.sky.CubemapSkybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // todo why is this black??? -> depth or backface fails :/
    val skybox = CubemapSkybox()
    skybox.imageFile = downloads.getChild("2d/cross cubemap.jpg")
    testSceneWithUI("CubemapSkybox", skybox)
}