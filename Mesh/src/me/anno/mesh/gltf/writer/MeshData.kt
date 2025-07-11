package me.anno.mesh.gltf.writer

import me.anno.cache.FileCacheList
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.io.files.FileReference

data class MeshData(
    val mesh: Mesh, val materials: FileCacheList<Material>,
    val animations: List<FileReference>
)