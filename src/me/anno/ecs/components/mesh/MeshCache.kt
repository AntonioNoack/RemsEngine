package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.ecs.prefab.change.Path
import me.anno.gpu.pipeline.Pipeline
import me.anno.io.files.FileReference
import me.anno.io.saveable.Saveable
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f

object MeshCache : PrefabByFileCache<Mesh>(Mesh::class, "Mesh") {

    private val LOGGER = LogManager.getLogger(MeshCache::class)

    override fun castInstance(instance: Saveable?, ref: FileReference): Mesh? {
        return when (instance) {
            is Mesh -> instance
            is MeshComponentBase -> instance.getMesh() as? Mesh
            is MeshSpawner -> joinMeshes(listOf(instance))
            is Entity -> {
                val components = ArrayList<Component>(64)
                fun forAll(entity: Entity) {
                    entity.validateTransform()
                    for (child in entity.children) {
                        if (child.isEnabled) {
                            forAll(child)
                        }
                    }
                    for (comp in entity.components) {
                        if (comp.isEnabled && (comp is MeshComponentBase || comp is MeshSpawner)) {
                            components.add(comp)
                        }
                    }
                }
                forAll(instance)
                joinMeshes(components)
            }
            null -> null
            else -> {
                LOGGER.warn("Requesting mesh from ${instance.className}, cannot extract it")
                null
            }
        }
    }

    private fun addMesh(
        meshes: ArrayList<Triple<Mesh, Transform?, List<FileReference>>>,
        mesh: IMesh?, transform: Transform?, compMaterials: List<FileReference>?
    ) {
        if (mesh is Mesh && mesh.proceduralLength <= 0) {
            val meshMaterials = mesh.materials
            val materials = (0 until mesh.numMaterials).map {
                Pipeline.getMaterialRef(compMaterials, meshMaterials, it)
            }
            meshes.add(Triple(mesh, transform, materials))
        }
    }

    /**
     * this should only be executed for decently small meshes ^^,
     * large meshes may cause OutOfMemoryExceptions
     * */
    private fun joinMeshes(list: List<Component>): Mesh? {

        val meshes = ArrayList<Triple<Mesh, Transform?, List<FileReference>>>()
        for (comp in list) {
            when (comp) {
                is MeshComponentBase -> {
                    addMesh(meshes, comp.getMesh(), comp.transform, comp.materials)
                }
                is MeshSpawner -> {
                    comp.forEachMesh { mesh, material, transform ->
                        val materialList = if (material == null) emptyList()
                        else listOf(material.ref)
                        addMesh(meshes, mesh, transform, materialList)
                    }
                }
            }
        }

        return when (meshes.size) {
            0 -> null
            1 -> {
                // special case: no joining required
                val (mesh, transform, materials) = meshes[0]
                transform?.validate()
                val matrix = transform?.globalTransform
                if ((matrix == null || matrix.isIdentity())) {
                    if (materials == mesh.materials) mesh else {
                        val clone = mesh.clone() as Mesh
                        clone.materials = materials
                        clone.prefabPath = Path.ROOT_PATH
                        clone.prefab = null
                        clone
                    }
                } else {
                    // transform required
                    // only needed for position, normal and tangents
                    val clone = mesh.clone() as Mesh
                    transformMesh(clone, matrix)
                    clone.materials = materials
                    clone.unlink()
                    clone
                }
            }
            else -> {

                val hasColors = meshes.any2 { it.first.color0 != null }
                val hasBones = meshes.any2 { it.first.hasBones }
                val hasUVs = meshes.any2 { it.first.uvs != null }

                object : MeshJoiner<Triple<Mesh, Transform?, List<FileReference>>>(hasColors, hasBones, hasUVs) {
                    override fun getMesh(element: Triple<Mesh, Transform?, List<FileReference>>) = element.first
                    override fun getMaterials(element: Triple<Mesh, Transform?, List<FileReference>>) = element.third
                    override fun getTransform(element: Triple<Mesh, Transform?, List<FileReference>>, dst: Matrix4x3f) {
                        val transform = element.second
                        if (transform != null) {
                            transform.validate()
                            dst.set(transform.globalTransform)
                        } else dst.identity()
                    }
                }.join(meshes)
            }
        }
    }

    fun transformMesh(mesh: Mesh, matrix: Matrix4x3d): Mesh {
        mesh.positions = transform(matrix, mesh.positions)
        mesh.normals = rotate(matrix, mesh.normals, 3)
        mesh.tangents = rotate(matrix, mesh.tangents, 4)
        mesh.invalidateGeometry()
        return mesh
    }

    private fun transform(matrix: Matrix4x3d, src: FloatArray?): FloatArray? {
        src ?: return null
        val tmp = JomlPools.vec3f.borrow()
        val dst = src.copyOf()
        for (i in src.indices step 3) {
            tmp.set(src, i)
            matrix.transformPosition(tmp)
            tmp.get(dst, i)
        }
        return dst
    }

    private fun rotate(matrix: Matrix4x3d, src: FloatArray?, stride: Int): FloatArray? {
        src ?: return null
        val tmp = JomlPools.vec3f.borrow()
        val dst = src.copyOf()
        for (i in src.indices step stride) {
            tmp.set(src, i)
            matrix.transformDirection(tmp)
                .safeNormalize()
            tmp.get(dst, i)
        }
        return dst
    }
}