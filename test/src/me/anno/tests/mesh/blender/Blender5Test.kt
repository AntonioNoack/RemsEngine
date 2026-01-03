package me.anno.tests.mesh.blender

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    // todo bug: our reader is using the vertices of Mesh 1 for Mesh 2, too, why/how???
    LogManager.disableLoggers("Saveable,ExtensionManager")
    LogManager.define("BlenderMeshConverter", Level.DEBUG)
    OfficialExtensions.initForTests()

    val file = documents.getChild("Blender/CompressionTest.blend")
    val mesh = MeshCache.getEntry(file).waitFor() as Mesh
    testSceneWithUI("Blender5", mesh)
}