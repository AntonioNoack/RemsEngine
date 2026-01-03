package me.anno.tests.mesh.blender

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.documents
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.disableLoggers("Saveable,ExtensionManager")
    LogManager.define("BlenderMaterialConverter", Level.DEBUG)
    OfficialExtensions.initForTests()

    val file = documents.getChild("Blender/Conditorei.blend")
    val mesh = MeshCache.getEntry(file).waitFor() as Mesh
    val material = MaterialCache.getEntry(mesh.materials[0]).waitFor() as Material
    println("Material: roughness: ${material.roughnessMinMax}, metallic: ${material.metallicMinMax}")
}