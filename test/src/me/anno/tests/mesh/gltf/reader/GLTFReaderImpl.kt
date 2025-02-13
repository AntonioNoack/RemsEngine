package me.anno.tests.mesh.gltf.reader

import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.inner.InnerFolder
import me.anno.mesh.gltf.GLTFReader
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {
    ECSRegistry.init()
    lateinit var folder: InnerFolder
    val src = OS.downloads.getChild("3d/azeria/scene.gltf")
    GLTFReader(src).readAnyGLTF(src.readBytesSync()) { folder1, err2 ->
        err2?.printStackTrace()
        folder = folder1!!
    }
    LogManager.disableInfoLogs("GFX,WindowManagement,OpenXRSystem,OpenXRUtils,Saveable,ExtensionManager")
    testSceneWithUI("GLTFReader", folder.getChild("Scene.json"))
    // testSceneWithUI("GLTFReader", folder.getChild("materials/fox_material.json"))
}
