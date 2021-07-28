package me.anno.mesh.vox.format

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.Path

class Layer(var name: String) {
    val nodes = ArrayList<Node>()
    fun containsModel(): Boolean {
        return nodes.any { it.containsModel() }
    }

    fun toEntity(meshes: List<Mesh>, index: Int): Entity {
        val entity = Entity(name.ifEmpty { "Layer $index" })
        for (node in nodes) {
            val child = node.toEntity(meshes)
            entity.add(child)
        }
        return entity
    }

    fun toEntityPrefab(changes: MutableList<Change>, meshes: List<Mesh>, index: Int) {
        changes.add(CAdd(Path(), 'e', "Entity"))
        changes.add(CSet(Path(index, 'e'), "name", name.ifEmpty { "Layer $index" }))
        for ((childIndex, node) in nodes.withIndex()) {
            node.toEntityPrefab(
                changes, meshes, Path(index, 'e'),
                Path(intArrayOf(index, childIndex), charArrayOf('e', 'e'))
            )
        }
    }

}