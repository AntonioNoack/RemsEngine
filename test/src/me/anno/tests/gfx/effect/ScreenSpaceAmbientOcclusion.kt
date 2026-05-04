package me.anno.tests.gfx.effect

import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.res

fun main() {
    val source = res.getChild("meshes/NavMesh.fbx")
    testSceneWithUI("SSAO", source, RenderMode.SSAO)
}