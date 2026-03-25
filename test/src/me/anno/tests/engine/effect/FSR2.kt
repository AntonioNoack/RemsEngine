package me.anno.tests.engine.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.RenderDoc
import me.anno.utils.OS.res

fun main() {
    RenderDoc.forceLoadRenderDoc()
    testSceneWithUI("FSR2", res.getChild("meshes/CuteGhost.fbx"), RenderMode.FSR2_X8)
}