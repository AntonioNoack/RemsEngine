package me.anno.tests.engine.sky

import me.anno.ecs.components.light.sky.UVSkybox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.image.ImageCache
import me.anno.utils.OS.downloads

fun main() {
    val src = downloads.getChild("2d/qwantani_1k.hdr")
    val skybox = UVSkybox()
    println(ImageCache[src, false])
    skybox.imageFile = src
    testSceneWithUI("UVSkybox", skybox)
}