package me.anno.tests.image

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS

fun main() {
    // done: roughness and metallic textures weren't loaded until full-reload
    testSceneWithUI("Helmet", OS.downloads.getChild("3d/DamagedHelmet.glb"))
}