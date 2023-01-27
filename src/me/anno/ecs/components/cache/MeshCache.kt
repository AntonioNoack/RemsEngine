package me.anno.ecs.components.cache

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.prefab.Prefab.Companion.maxPrefabDepth
import me.anno.ecs.prefab.PrefabByFileCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.arrays.ExpandingByteArray
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Matrices.set2
import me.anno.utils.types.Vectors.safeNormalize
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import org.joml.Vector3f
import kotlin.math.min

object MeshCache : PrefabByFileCache<Mesh>(Mesh::class) {

    private val LOGGER = LogManager.getLogger(MeshCache::class)

    private val cache = CacheSection("MeshCache2")

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

    /**
     * this should only be executed for decently small meshes ^^,
     * large meshes might cause OutOfMemoryExceptions
     * */
    private fun joinMeshes(list: Iterable<Component>): Mesh? {

        val positions = ExpandingFloatArray(256)
        val normals = ExpandingFloatArray(256)
        val tangents = ExpandingFloatArray(256)
        val materialIds = ExpandingIntArray(256)
        val indices = ExpandingIntArray(256)
        val uvs = ExpandingFloatArray(256)
        val color0 = ExpandingIntArray(256)
        val boneWeights = ExpandingFloatArray(256)
        val boneIndices = ExpandingByteArray(256)

        val materials = HashMap<FileReference, Int>()


        // we could optimize the case of no indices being present...

        val trans = Matrix4x3f()
        val tmp = Vector3f()
        fun addMesh(mesh: Mesh, transform: Transform?, getMaterial: (Int) -> FileReference) {

            val pos = mesh.positions ?: return
            if (pos.isEmpty()) return

            if (transform != null) {
                transform.validate()
                trans.set2(transform.globalTransform)
            } else trans.identity()

            mesh.ensureNorTanUVs()

            // add mesh with all materials and all properties...
            // only initialize, what is necessary

            val numTris = mesh.numPrimitives
            val idx = mesh.indices

            // a lot here depends on correct sizes ->
            // be careful when adding data, to not add too much or too little

            val prevNumVertices = positions.size / 3
            val numVertices = if (idx == null) pos.size / 9 * 3 else pos.size / 3
            positions.ensureExtra(numVertices * 3)
            for (i in 0 until numVertices) {
                val i3 = i * 3
                tmp.set(pos, i3)
                positions.add(trans.transformPosition(tmp))
            }

            normals.ensureExtra(normals.size)
            val nor = mesh.normals!!
            for (i in 0 until numVertices) {
                val i3 = i * 3
                tmp.set(nor, i3)
                trans.transformDirection(tmp).safeNormalize()
                normals.add(tmp)
            }

            val uv = mesh.uvs
            if (uv != null) {
                // pad existing
                uvs.skip(prevNumVertices * 2 - uvs.size)
                uvs.addAll(uv, 0, min(uv.size, numVertices * 2))
            }

            val tan = mesh.tangents
            if (tan != null) {
                // pad existing
                tangents.skip(prevNumVertices * 4 - tangents.size)
                tangents.ensureExtra(min(tan.size, numVertices * 4))
                for (i in 0 until min(tan.size / 4, numVertices)) {
                    val i4 = i * 4
                    tmp.set(tan, i4)
                    trans.transformDirection(tmp).safeNormalize()
                    tangents.add(tmp)
                    tangents.add(tan[i4 + 3])
                }
            }

            // todo more colors?
            val col = mesh.color0
            if (col != null) {
                color0.skip(prevNumVertices - color0.size)
                color0.add(col, 0, min(col.size, numVertices))
            }

            val mat = mesh.materialIds
            materialIds.ensureExtra(numTris)
            if (mat == null) {
                val matId: Int = materials.getOrPut(getMaterial(0)) { materials.size }
                for (i in 0 until numTris) {
                    materialIds.add(matId)
                }
            } else {
                // todo could be made more efficient by caching the mapping
                for (i in 0 until numTris) {
                    val matId = materials.getOrPut(getMaterial(mat[i])) { materials.size }
                    materialIds.add(matId)
                }
            }

            val weiW = mesh.boneWeights
            val weiI = mesh.boneIndices
            if (weiW != null && weiI != null) {
                // pad correct amount of missing values
                boneWeights.skip(prevNumVertices * 4 - boneWeights.size)
                boneIndices.skip(prevNumVertices * 4 - boneIndices.size)
                // append bone weights & indices
                boneWeights.addAll(weiW, 0, min(weiW.size, numVertices * 4))
                boneIndices.addAll(weiI, 0, min(weiI.size, numVertices * 4))
            }

            if (idx != null) {
                // just append it, with correct offset
                for (i in 0 until idx.size / 3 * 3) {
                    indices.add(prevNumVertices + idx[i])
                }
            } else {
                // generate new indices 0 1 2 3 4 5..., with correct offset
                for (i in prevNumVertices until prevNumVertices + numTris * 3) {
                    indices.add(i)
                }
            }

        }

        for (comp in list) {
            when (comp) {
                is MeshComponentBase -> {
                    val mesh = comp.getMesh() ?: continue
                    if (mesh.proceduralLength > 0) continue
                    val mat = comp.materials
                    addMesh(mesh, comp.transform) {
                        mat.getOrNull(it)?.nullIfUndefined() ?: mesh.materials.getOrNull(it) ?: InvalidRef
                    }
                }
                is MeshSpawner -> {
                    comp.forEachMesh { mesh, material, transform ->
                        if (mesh.proceduralLength <= 0)
                            addMesh(mesh, transform) { material?.ref ?: InvalidRef }
                    }
                }
            }
        }

        if (positions.size == 0) return null

        val mesh = Mesh()

        val numPos = positions.size / 3
        val numTris = indices.size / 3

        mesh.positions = positions.toFloatArray()
        mesh.normals = normals.toFloatArray()
        mesh.tangents = if (tangents.size > 0) tangents.toFloatArray() else null
        mesh.materialIds = if (materialIds.isNotEmpty()) materialIds.toIntArray(numTris) else null
        mesh.materials = materials.entries.sortedBy { it.value }.map { it.key }
        mesh.indices = indices.toIntArray()
        mesh.uvs = if (uvs.size > 0) uvs.toFloatArray(numPos * 2) else null
        mesh.color0 = if (color0.isNotEmpty()) color0.toIntArray() else null
        if (boneWeights.size > 0 && boneIndices.size > 0) {
            mesh.boneWeights = boneWeights.toFloatArray()
            mesh.boneIndices = boneIndices.toByteArray()
        }

        return mesh

    }

}