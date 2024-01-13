package me.anno.tests.mesh.blender

import me.anno.engine.PluginRegistry
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.utils.OS.downloads

fun main() {
    PluginRegistry.init()
    ExtensionLoader.load()
    testSceneWithUI("Bone Indices Renderer", downloads.getChild("3d/Talking On Phone 2.fbx")) {
        it.renderer.renderMode = RenderMode.BONE_INDICES
    }
}