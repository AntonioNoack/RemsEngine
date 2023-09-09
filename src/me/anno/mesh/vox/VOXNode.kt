package me.anno.mesh.vox

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import org.apache.logging.log4j.LogManager
import org.joml.Matrix3f
import org.joml.Quaterniond
import org.joml.Vector3d

class VOXNode {

    // meta data
    var name = ""

    // hierarchy
    var child: VOXNode? = null
    var children: List<VOXNode>? = null
    var models: IntArray? = null

    // transform
    var px = 0.0
    var py = 0.0
    var pz = 0.0
    var rotation = 0

    fun containsModel(): Boolean {
        return if ((models ?: child?.models) != null) true
        else children?.any { it.containsModel() } ?: false
    }

    fun toEntityPrefab(prefab: Prefab, meshes: List<FileReference>, parentPath: Path, entityIndex: Int) {
        val path = prefab.add(parentPath, 'e', "Entity", name, entityIndex)
        val models = models ?: child?.models
        if (models != null) {
            // add these models as components
            for (i in models) {
                val mesh = meshes[i]
                val name = mesh.name
                if ((0 until i).any { meshes[it].name == name }) LOGGER.warn("Duplicate name! $name")
                val meshComponent = prefab.add(path, 'c', "MeshComponent", name)
                prefab.setUnsafe(meshComponent, "meshFile", mesh)
            }
        }
        if (px != 0.0 || py != 0.0 || pz != 0.0) {
            prefab.setUnsafe(path, "position", Vector3d(px, py, pz))
            // rotation is found, but looks wrong in the price of persia sample
        }
        val rotation = rotation
        if (rotation in 1 until rotations.size) {
            prefab.setUnsafe(path, "rotation", rotations[rotation])
        }
        val children = children
        if (children != null) {
            val names = HashSet<String>(children.size)
            for (i in children.indices) {
                val child = children[i]
                while (!names.add(child.name)) {
                    // not ideal
                    child.name += '_'
                }
                children[i].toEntityPrefab(prefab, meshes, path, i)
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(VOXNode::class)

        /*
        * rotation encoding:
        *   bit | meaning
        *   0-1 : index of the non-zero entry in the first row
        *   2-3 : index of the non-zero entry in the second row
        *   4   : the sign in the first row (0 : positive; 1 : negative)
        *   5   : the sign in the second row (0 : positive; 1 : negative)
        *   6   : the sign in the third row (0 : positive; 1 : negative)
        * */
        private val tmp = Matrix3f()
        private val rot0 = Quaterniond()
        private val rotations = Array(128) { ry ->
            // only decode valid rotations
            if (ry.and(3) != ry.shr(2).and(3) &&
                ry.and(3) != 3 &&
                ry.and(12) != 12
            ) {
                // this also carries scale information...
                // but is probably always will be zero :)
                val rot1 = Quaterniond()
                val rot0 = tmp.set(
                    0f, 0f, 0f,
                    0f, 0f, 0f,
                    0f, 0f, 0f
                )
                val v0 = if (ry.and(16) != 0) -1f else +1f
                when (ry.and(3)) {
                    0 -> rot0.m00 = v0
                    1 -> rot0.m01 = v0
                    2 -> rot0.m02 = v0
                }
                val v1 = if (ry.and(32) != 0) -1f else +1f
                when (ry.shr(2).and(3)) {
                    0 -> rot0.m10 = v1
                    1 -> rot0.m11 = v1
                    2 -> rot0.m12 = v1
                }
                val v2 = if (ry.and(64) != 0) -1f else +1f
                when {
                    rot0.m00 == 0f && rot0.m10 == 0f -> rot0.m20 = v2
                    rot0.m01 == 0f && rot0.m11 == 0f -> rot0.m21 = v2
                    else -> rot0.m22 = v2
                }
                rot0.getNormalizedRotation(rot1)
                // rotation correction for y<->z
                val t = rot1.y
                rot1.y = rot1.z
                rot1.z = -t
                rot1
            } else rot0
        }
    }

}