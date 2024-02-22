package me.anno.mesh.vox

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.utils.structures.lists.Lists.any2

class VOXLayer(var name: String) {

    val nodes = ArrayList<VOXNode>()

    fun containsModel(): Boolean {
        return nodes.any2 { it.containsModel() }
    }

    fun toEntityPrefab(prefab: Prefab, meshes: List<FileReference>, index: Int) {
        val name = name.ifEmpty { "Layer $index" }
        val entity = prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", name), index, -1)
        prefab.setUnsafe(entity, "name", name)
        val names = HashSet<String>()
        for ((childIndex, child) in nodes.withIndex()) {
            while (!names.add(child.name)) {
                // not ideal
                child.name += '_'
            }
            child.toEntityPrefab(
                prefab, meshes, entity,
                childIndex
            )
        }
    }
}