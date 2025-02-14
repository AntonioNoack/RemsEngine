package me.anno.tests.mesh.gltf.reader

import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.inner.InnerFolder
import me.anno.mesh.gltf.GLTFReader
import me.anno.utils.OS

fun main() {
    // todo this scene is using KHR_draco_mesh_compression, implement and test it
    //  -> draco is hellish-ly complicated, has bad documentation, and no existing Java ports :/.
    //  -> even JS runs on C++ via WASM
    ECSRegistry.init()
    lateinit var folder: InnerFolder
    val src = OS.downloads.getChild("3d/LittlestTokyo.glb")
    GLTFReader(src).readAnyGLTF(src.readBytesSync()) { folder1, err2 ->
        err2?.printStackTrace()
        folder = folder1!!
    }
    testSceneWithUI("GLTFReader", folder.getChild("Scene.json"))
}
