package me.anno.tests.engine.effect

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

// implement screen space global illumination :3
//  - like in SSAO, we're tracing rays from A to B
//  - we need to "accumulate" color / emissive
//  - we need to sample randomly, so should/can we do that temporarily?
//      -> we probably need TAA first
// we need to do that recursively a few times -> nah, 1 bounce could be good enough

fun main() {
    OfficialExtensions.initForTests()
    val src = downloads.getChild("3d/ogldev-source/crytek_sponza/sponza.fbx")
    testSceneWithUI("SSGI", src) {
        it.renderer.renderMode = RenderMode.SSGI
    }
}