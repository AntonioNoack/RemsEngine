package me.anno.mesh.vox.format

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.ChangeAddEntity
import me.anno.ecs.prefab.ChangeSetEntityAttribute
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
        changes.add(ChangeAddEntity(Path()))
        changes.add(ChangeSetEntityAttribute(Path(index, "name"), name.ifEmpty { "Layer $index" }))
        for ((childIndex, node) in nodes.withIndex()) {
            node.toEntityPrefab(changes, meshes, intArrayOf(index), intArrayOf(index, childIndex))
        }
    }

}