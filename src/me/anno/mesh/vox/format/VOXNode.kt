package me.anno.mesh.vox.format

import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.Prefab
import me.anno.io.files.FileReference
import org.joml.Vector3d

class VOXNode {

    // meta data
    var name: String = ""

    // hierarchy
    var child: VOXNode? = null
    var children: List<VOXNode>? = null
    var models: IntArray? = null

    // transform
    var px = 0.0
    var py = 0.0
    var pz = 0.0
    var ry = 0.0

    fun containsModel(): Boolean {
        return if (models ?: child?.models != null) true
        else children?.any { it.containsModel() } ?: false
    }

    fun toEntityPrefab(prefab: Prefab, meshes: List<FileReference>, parentPath: Path, entityIndex: Int) {
        val path = prefab.add(CAdd(parentPath, 'e', "Entity", name), entityIndex)
        val models = models ?: child?.models
        if (models != null) {
            // add these models as components
            for (i in models) {
                val mesh = meshes[i]
                val meshComponent = prefab.add(CAdd(path, 'c', "MeshComponent", mesh.name), i)
                prefab.add(CSet(meshComponent, "mesh", mesh))
            }
        }
        if (px != 0.0 || py != 0.0 || pz != 0.0) {
            prefab.add(CSet(path, "position", Vector3d(px, py, pz)))
            // rotation is found, but looks wrong in the price of persia sample
        }
        val children = children
        if (children != null) {
            for (childIndex in children.indices) {
                children[childIndex].toEntityPrefab(prefab, meshes, path, childIndex)
            }
        }
    }

}