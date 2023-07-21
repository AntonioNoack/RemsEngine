package me.anno.ecs.components.mesh

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f

object MeshCache : PrefabByFileCache<Mesh>(Mesh::class) {

    private val LOGGER = LogManager.getLogger(MeshCache::class)

    val cache = CacheSection("MeshCache2")

    override operator fun get(ref: FileReference?, async: Boolean): Mesh? {
        if (ref == null || ref == InvalidRef) return null
        ensureMeshClasses()
        val value0 = lru[ref]
        if (value0 !== Unit) return value0 as? Mesh
        val data = cache.getFileEntry(ref, false, PrefabCache.prefabTimeout, async) { ref1, _ ->
            val mesh: Mesh? = when (val instance = PrefabCache.getPrefabInstance(ref1, maxPrefabDepth, async)) {
                is Mesh -> instance
                is MeshComponent -> {
                    // warning: if there is a dependency ring, this will produce a stack overflow
                    val ref2 = instance.mesh
                    if (ref == ref2) null
                    else get(ref2, async)
                }
                is MeshComponentBase -> instance.getMesh()
                is Entity -> {
                    instance.forAll { if (it is Entity) it.validateTransform() }
                    val seq = ArrayList<Component>(64)
                    instance.forAll {
                        if (it is MeshComponentBase || it is MeshSpawner) {
                            seq.add(it as Component)
                        }
                    }
                    joinMeshes(seq)
                }
                is MeshSpawner -> joinMeshes(listOf(instance))
                null -> null
                else -> {
                    LOGGER.warn("Requesting mesh from ${instance.className}, cannot extract it")
                    null
                }
            }
            CacheData(mesh)
        } as? CacheData<*>
        val value = data?.value as? Mesh
        lru[ref] = value
        return value
    }

    private fun addMesh(
        meshes: ArrayList<Triple<Mesh, Transform?, List<FileReference>>>,
        mesh: Mesh?, transform: Transform?, compMaterials: List<FileReference?>?
    ) {
        if (mesh != null && mesh.proceduralLength <= 0) {
            val meshMaterials = mesh.materials
            val materials = (0 until mesh.numMaterials).map {
                compMaterials?.getOrNull(it)?.nullIfUndefined() ?: meshMaterials.getOrNull(it) ?: InvalidRef
            }
            meshes.add(Triple(mesh, transform, materials))
        }
    }

    /**
     * this should only be executed for decently small meshes ^^,
     * large meshes might cause OutOfMemoryExceptions
     * */
    private fun joinMeshes(list: Iterable<Component>): Mesh {

        val meshes = ArrayList<Triple<Mesh, Transform?, List<FileReference>>>()
        for (comp in list) {
            when (comp) {
                is MeshComponentBase -> addMesh(meshes, comp.getMesh(), comp.transform, comp.materials)
                is MeshSpawner -> {
                    comp.forEachMesh { mesh, material, transform ->
                        addMesh(meshes, mesh, transform, listOf(material?.ref))
                    }
                }
            }
        }

        val hasColors = meshes.any2 { it.first.color0 != null }
        val hasBones = meshes.any2 { it.first.hasBones }
        val hasUVs = meshes.any2 { it.first.uvs != null }

        return object : MeshJoiner<Triple<Mesh, Transform?, List<FileReference>>>(hasColors, hasBones, hasUVs) {
            override fun getMesh(element: Triple<Mesh, Transform?, List<FileReference>>) = element.first
            override fun getMaterials(element: Triple<Mesh, Transform?, List<FileReference>>) = element.third
            override fun getTransform(element: Triple<Mesh, Transform?, List<FileReference>>, dst: Matrix4x3f) {
                val transform = element.second
                if (transform != null) dst.set(transform.globalTransform)
                else dst.identity()
            }
        }.join(Mesh(), meshes)

    }

}