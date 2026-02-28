package me.anno.engine.ui

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.TransformMesh.scale
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.projects.FileEncoding
import me.anno.engine.projects.GameEngineProject.Companion.invalidateThumbnails
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.lists.Lists.none2
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f

object MeshUtils {

    private val LOGGER = LogManager.getLogger(MeshUtils::class)

    fun scaleMeshes(files: List<FileReference>, scale: Float) {
        for (file in files) {
            if (file.isDirectory) {
                file.listChildren { children, _ ->
                    scaleMeshes(children ?: emptyList(), scale)
                }
            } else scaleMesh(file, scale)
        }
    }

    fun scaleMesh(file: FileReference, scale: Float) {
        val decoder = FileEncoding.getForExtension(file.lcExtension, false)
            ?: run {
                LOGGER.warn("Skipped file, because of extension mismatch: $file")
                return
            }

        file.readBytes { bytes, _ ->
            if (bytes == null) {
                LOGGER.warn("Could not read bytes from $file")
                return@readBytes
            }

            val instances = decoder.decode(bytes, workspace, true)
            if (instances.none2 { it is Mesh || it is Prefab && it.clazzName == "Mesh" }) {
                LOGGER.warn("Could not find meshes in $file")
                return@readBytes
            }

            val distinctMeshes = instances.filterIsInstance2(Mesh::class).distinct()
            distinctMeshes.forEach { mesh -> mesh.scale(Vector3f(scale)) }

            val distinctPrefabs = instances.filterIsInstance2(Prefab::class)
                .filter { prefab -> prefab.clazzName == "Mesh" && prefab["positions"] is FloatArray }.distinct()
            distinctPrefabs.forEach { prefab ->
                // todo push change to history...
                val positions = prefab["positions"] as FloatArray
                for (i in positions.indices) {
                    positions[i] *= scale
                }
            }

            val encoded = decoder.encode(instances, workspace)
            file.writeBytes(encoded)
            LOGGER.info("Scaled $file by $scale, ${encoded.size.formatFileSize()}")

            // invalidate all its dependent assets...
            invalidateThumbnails(listOf(file))
        }
    }

    fun importInplace(files: List<FileReference>) {
        for (file in files) {
            if (file.isDirectory) {
                file.listChildren { children, _ ->
                    importInplace(children ?: emptyList())
                }
            } else importInplace(file)
        }
    }

    fun importInplace(file: FileReference) {
        val decoder = FileEncoding.getForExtension(file.lcExtension, false)
            ?: run {
                LOGGER.warn("Skipped file, because of extension mismatch: $file")
                return
            }

        file.readBytes { bytes, _ ->
            if (bytes == null) {
                LOGGER.warn("Could not read bytes from $file")
                return@readBytes
            }

            val instances = decoder.decode(bytes, workspace, true)
            if (instances.none2 { it is Prefab && it.isWritable }) {
                LOGGER.warn("Could not find prefabs in $file")
                return@readBytes
            }

            val distinctPrefabs = instances.filterIsInstance2(Prefab::class)
                .filter { it.isWritable }
                .distinct()

            distinctPrefabs.forEach { prefab ->
                // todo push change to history...
                for (adds in prefab.adds.values) {
                    for ((index, add) in adds.withIndex()) {
                        val src = add.prefab
                        if (src == InvalidRef) continue

                        val path = add.getSetterPath(index)

                        val srcPrefab = PrefabCache[src].waitFor()?.prefab ?: continue
                        for ((srcPath, srcAdds) in srcPrefab.adds) {
                            for (srcAdd in srcAdds) {
                                prefab.add(srcAdd.withPath(path + srcPath, false), index)
                            }
                        }

                        srcPrefab.sets.forEach { srcPath, key, value ->
                            prefab[path + srcPath, key] = value
                        }

                        // we're now standing on our own feet
                        add.clazzName = srcPrefab.clazzName
                        add.prefab = InvalidRef
                    }
                }
            }

            val encoded = decoder.encode(instances, workspace)
            file.writeBytes(encoded)
            LOGGER.info("Import-inplace-d $file, ${encoded.size.formatFileSize()}")

            // invalidate all its dependent assets...
            invalidateThumbnails(listOf(file))
        }
    }

}