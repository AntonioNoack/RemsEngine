package me.anno.tests.mesh.blender

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    // todo check whether glass bdrf node is working
    // todo check that IOR is assigned
    OfficialExtensions.initForTests()
    LogManager.define("BlenderMeshConverter", Level.DEBUG)
    val mesh = MeshCache[documents.getChild("Blender/GlassMaterialTest.blend"), false]!!
    println(mesh)
    if (false) testSceneWithUI("Blender Glass", mesh)
    Engine.requestShutdown()
}