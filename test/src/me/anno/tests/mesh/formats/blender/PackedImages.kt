package me.anno.tests.mesh.blender

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.ECSFileExplorer
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.image.ImageCache
import me.anno.mesh.blender.BlenderReader
import me.anno.tests.io.files.printTree
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures

fun main() {
    OfficialExtensions.initForTests()
    val file = documents.getChild("Blender/PackSample.blend")
    BlenderReader.readAsFolder(file) { folder, exc ->
        exc?.printStackTrace()
        folder?.printTree(1, 10)
    }
    if (false) {
        ImageCache[file.getChild("textures/Test Image.png")].waitFor()!!
            .write(desktop.getChild("Test Image.png"))
        ImageCache[pictures.getChild("Cracked Normals2.webp")].waitFor()!!
            .write(desktop.getChild("Cracked.png"))
    }
    if (true) {
        ECSRegistry.init()
        if (false) {
            val mesh = MeshCache.getEntry(file).waitFor() as? Mesh
            println(mesh?.uvs)
            println(mesh?.normals)
            println(mesh?.positions)
            println(mesh?.ensureNorTanUVs())
            println(mesh?.invalidateGeometry())
            testSceneWithUI("PackedImages", mesh!!.ref)
        }
        testUI3("PackedImages2") {
            ECSFileExplorer(file, style)
        }
    }
}