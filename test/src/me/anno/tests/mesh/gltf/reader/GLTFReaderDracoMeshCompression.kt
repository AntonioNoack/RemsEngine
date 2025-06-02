package me.anno.tests.mesh.gltf.reader

import kotlinx.coroutines.runBlocking
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.gltf.GLTFReader
import me.anno.utils.OS

fun main() {
    // todo this scene is using KHR_draco_mesh_compression, implement and test it
    //  -> draco is hellish-ly complicated, has bad documentation, and no existing Java ports :/.
    //  -> even JS runs on C++ via WASM
    ECSRegistry.init()

    val src = OS.downloads.getChild("3d/LittlestTokyo.glb")
    val folder = runBlocking {
        val bytes = src.readBytes().getOrThrow()
        GLTFReader(src).readAnyGLTF(bytes).getOrThrow()
    }
    testSceneWithUI("GLTFReader", folder.getChild("Scene.json"))
}
