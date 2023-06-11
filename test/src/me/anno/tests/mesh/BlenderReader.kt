package me.anno.tests.mesh

import me.anno.ecs.components.mesh.MeshCache
import me.anno.utils.OS

fun main() {
    val file = OS.documents.getChild("Blender/VolumetricHair.blend")
    MeshCache[file, false]!!
}