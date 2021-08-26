package me.anno.mesh.vox.format

import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.Path
import me.anno.io.files.FileReference

class VOXLayer(var name: String) {

    val nodes = ArrayList<VOXNode>()

    fun containsModel(): Boolean {
        return nodes.any { it.containsModel() }
    }

    fun toEntityPrefab(changes: MutableList<Change>, meshes: List<FileReference>, index: Int) {
        val name = name.ifEmpty { "Layer $index" }
        val entity = CAdd(Path.ROOT_PATH, 'e', "Entity", name)
        changes.add(entity)
        val path = entity.getChildPath(index)
        changes.add(CSet(path, "name", name))
        for ((childIndex, node) in nodes.withIndex()) {
            node.toEntityPrefab(
                changes, meshes, path,
                childIndex
            )
        }
    }

}