package me.anno.tests.mesh.blender

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.documents
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    OfficialExtensions.initForTests()
    LogManager.define("BlenderMeshConverter", Level.DEBUG)
    val file = documents.getChild("Blender/CompressionTest.blend")
    val mesh = MeshCache.getEntry(file).waitFor() as Mesh
    println("Mesh: ${mesh.numPrimitives} tris, ${mesh.positions?.size} pos, ${mesh.indices?.size} indices")
    println("Bounds: ${mesh.getBounds()}")
    println("Positions: ${mesh.positions?.toList()}")
    // testSceneWithUI("Blender5", mesh)
}