package me.anno.tests.gfx.textures

import me.anno.Build
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // todo investigate: there's tons of messages like
    //  Mapping 'ggg1' failed, because Texture2D("i2t/?/71.png"@0, 2048 x 2048 x 1, RGBA8) is destroyed
    Build.isDebug = false // disable glGetError()
    testSceneWithUI("GLB missing textures", downloads.getChild("The Junk Shop.glb"))
}
