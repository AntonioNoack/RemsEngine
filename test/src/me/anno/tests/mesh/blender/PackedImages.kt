package me.anno.tests.mesh.blender

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.engine.PluginRegistry
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.extensions.ExtensionLoader
import me.anno.image.ImageCache
import me.anno.mesh.blender.BlenderReader
import me.anno.tests.files.printTree
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures

fun main() {
    PluginRegistry.init()
    ExtensionLoader.load()
    val file = documents.getChild("Blender/PackSample.blend")
    BlenderReader.readAsFolder(file) { folder, exc ->
        exc?.printStackTrace()
        folder?.printTree(1, 10)
    }
    if (false) {
        ImageCache[file.getChild("textures/Test Image.png"), false]!!
            .write(desktop.getChild("Test Image.png"))
        ImageCache[pictures.getChild("Cracked Normals2.webp"), false]!!
            .write(desktop.getChild("Cracked.png"))
    }
    if (true) {
        ECSRegistry.init()
        if (false) {
            println(MeshCache[file, false]?.uvs)
            println(MeshCache[file, false]?.normals)
            println(MeshCache[file, false]?.positions)
            println(MeshCache[file, false]?.ensureNorTanUVs())
            println(MeshCache[file, false]?.invalidateGeometry())
            testSceneWithUI("PackedImages", MeshCache[file, false]!!.ref)
        }
        testUI3("PackedImages2") {
            ECSFileExplorer(file, style)
        }
    }
}