package me.anno.tests.mesh.gltf.reader

import kotlinx.coroutines.runBlocking
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.gltf.GLTFReader
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {
    ECSRegistry.init()
    val src = OS.downloads.getChild("3d/azeria/scene.gltf")
    val folder = runBlocking {
        val bytes = src.readBytes().getOrThrow()
        GLTFReader(src).readAnyGLTF(bytes).getOrThrow()
    }
    LogManager.disableInfoLogs("GFX,WindowManagement,OpenXRSystem,OpenXRUtils,Saveable,ExtensionManager")
    testSceneWithUI("GLTFReader", folder.getChild("Scene.json"))
    // testSceneWithUI("GLTFReader", folder.getChild("materials/fox_material.json"))
}
