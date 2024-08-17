package me.anno.tests.engine.material

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

/**
 * Sponza looked weird, so I investigated: it was using bump maps for normal maps
 * After this, bump maps are now supported; BUT please use normal maps, if you have them.
 * They are higher quality.
 * */
fun main() {
    testSceneWithUI("NormalMaps", downloads.getChild("3d/ogldev-source/crytek_sponza/sponza.fbx"))
}