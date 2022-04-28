package me.anno.mesh.vox.format

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference

class VOXLayer(var name: String) {

    val nodes = ArrayList<VOXNode>()

    fun containsModel(): Boolean {
        return nodes.any { it.containsModel() }
    }

    fun toEntityPrefab(prefab: Prefab, meshes: List<FileReference>, index: Int) {
        val name = name.ifEmpty { "Layer $index" }
        val entity = prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", name), index, -1)
        prefab.setUnsafe(entity, "name", name)
        for ((childIndex, node) in nodes.withIndex()) {
            node.toEntityPrefab(
                prefab, meshes, entity,
                childIndex
            )
        }
    }

}