package me.anno.mesh.gltf.writer

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.files.FileReference

data class MeshData(
    val mesh: Mesh, val materials: List<FileReference>,
    val animations: List<FileReference>
)