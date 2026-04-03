package me.anno.tests.mesh.blender

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.engine.OfficialExtensions
import me.anno.io.files.Reference.getReference
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    LogManager.disableLoggers("Saveable,ExtensionManager")
    LogManager.define("BlenderMaterialConverter", Level.DEBUG)
    OfficialExtensions.initForTests()

    val file = getReference("/mnt/Windows/Users/Antonio/Documents/Blender/AbbeanumFlur.blend")
    val mesh = MeshCache.getEntry(file).waitFor() as Mesh
    for (materialRef in mesh.materials) {
        val material = MaterialCache.getEntry(materialRef).waitFor() as? Material ?: continue
        println(
            "Material: roughness: ${material.roughnessMinMax}, metallic: ${material.metallicMinMax}, " +
                    "diffuse: ${material.diffuseMap}, emissive: ${material.emissiveMap}"
        )
    }
}