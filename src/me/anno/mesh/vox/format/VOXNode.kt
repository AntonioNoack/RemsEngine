package me.anno.mesh.vox.format

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Change
import me.anno.ecs.prefab.Path
import me.anno.io.files.FileReference
import org.joml.Vector3d

// todo read vox files as directory just like other meshes
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

    fun toEntity(meshes: List<Mesh>): Entity {
        val entity = Entity(name)
        val models = models ?: child?.models
        if (models != null) {
            // add these models as components
            for (modelIndex in models) {
                val mesh = meshes[modelIndex]
                entity.add(mesh.clone())
            }
            entity.add(MeshRenderer())
        }
        if (px != 0.0 || py != 0.0 || pz != 0.0) {
            entity.transform.localPosition = Vector3d(px, py, pz)
            // rotation is found, but looks wrong in the price of persia sample
        }
        val children = children
        if (children != null) {
            for (child in children) {
                entity.add(child.toEntity(meshes))
            }
        }
        return entity
    }

    fun toEntityPrefab(changes: MutableList<Change>, meshes: List<FileReference>, parentPath: Path, entityIndex: Int) {
        val entity = CAdd(parentPath, 'e', "Entity", name)
        changes.add(entity)
        val path = entity.getChildPath(entityIndex)
        val models = models ?: child?.models
        if (models != null) {
            // add these models as components
            for (modelIndex in models) {
                val mesh = meshes[modelIndex]
                changes.add(CAdd(path, 'c', "Mesh", mesh.nameWithoutExtension, mesh))
            }
            changes.add(CAdd(path, 'c', "MeshRenderer", "MeshRenderer"))
        }
        if (px != 0.0 || py != 0.0 || pz != 0.0) {
            changes.add(CSet(path, "position", Vector3d(px, py, pz)))
            // rotation is found, but looks wrong in the price of persia sample
        }
        val children = children
        if (children != null) {
            for (childIndex in children.indices) {
                children[childIndex].toEntityPrefab(changes, meshes, path, childIndex)
            }
        }
    }

}