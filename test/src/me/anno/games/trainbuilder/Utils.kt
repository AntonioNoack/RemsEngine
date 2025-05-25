package me.anno.games.trainbuilder

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.Material.Companion.defaultMaterial
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.gpu.CullMode
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.maps.LazyMap

fun List<String>.mapChildren(folder: FileReference): List<FileReference> {
    return map { name -> folder.getChild(name) }
}

private val flippedMaterials = LazyMap<FileReference, FileReference> { src ->
    val original = MaterialCache[src] ?: defaultMaterial
    val flipped = original.clone() as Material
    flipped.cullMode = CullMode.BACK
    flipped.ref
}

fun mirrorX(file: FileReference): FileReference {
    val original = MeshCache[file]!!
    val flippedMaterials = (0 until original.numMaterials)
        .map { idx -> flippedMaterials[original.materials.getOrNull(idx) ?: InvalidRef] }
    // todo why are the normals flipped upside down???
    return Entity()
        .setScale(-1f, 1f, 1f)
        .add(MeshComponent(file).apply {
            materials = flippedMaterials
        })
        .ref
}
