package me.anno.tests.mesh.blender

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    // check whether glass bdrf node is working -> yes
    // check that IOR is assigned -> yes
    // check that material IDs are working -> yes :D
    OfficialExtensions.initForTests()
    LogManager.define("BlenderMeshConverter", Level.DEBUG)
    val mesh = MeshCache.getEntry(documents.getChild("Blender/GlassMaterialTest.blend")).waitFor() as Mesh
    testSceneWithUI("Blender Glass", mesh)
    Engine.requestShutdown()
}