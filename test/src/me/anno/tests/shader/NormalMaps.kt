package me.anno.tests.shader

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // Sponza looked weird, so I investigated: it was using bump maps for normal maps
    testSceneWithUI("NormalMaps", downloads.getChild("ogldev-source/crytek_sponza/sponza.fbx"))
}