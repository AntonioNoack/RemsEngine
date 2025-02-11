package me.anno.tests.mesh.gltf

import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.inner.InnerFolder
import me.anno.mesh.gltf.GLTFReader
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {
    testReadAzeria()
}

fun testReadAzeria() {
    lateinit var folder: InnerFolder
    val src = OS.downloads.getChild("3d/azeria/scene.gltf")
    GLTFReader(src).readAnyGLTF(src.inputStreamSync()) { folder1, err2 ->
        err2?.printStackTrace()
        folder = folder1!!
    }
    LogManager.disableInfoLogs("GFX,WindowManagement,OpenXRSystem,OpenXRUtils,Saveable,ExtensionManager")
    testSceneWithUI("GLTFReader", folder.getChild("meshes/0.json"))
    // testSceneWithUI("GLTFReader", folder.getChild("materials/fox_material.json"))
}

fun testReadWrite() {
    val mesh = DefaultAssets.uvSphere
    lateinit var folder: InnerFolder
    GLTFWriter().write(mesh) { bytes, err ->
        err?.printStackTrace()
        val asBytes = bytes!!
        println(String(asBytes))
        GLTFReader(mesh.ref).readAnyGLTF(asBytes.inputStream()) { folder1, err2 ->
            err2?.printStackTrace()
            folder = folder1!!
        }
    }
    testSceneWithUI("GLTFReader", folder.getChild("meshes/0.json"))
}