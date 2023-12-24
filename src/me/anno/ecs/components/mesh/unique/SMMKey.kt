package me.anno.ecs.components.mesh.unique

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponentBase

data class SMMKey(val comp: MeshComponentBase, val mesh: Mesh, val index: Int)