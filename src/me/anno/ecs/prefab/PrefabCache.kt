package me.anno.ecs.prefab

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.unity.UnityReader
import me.anno.io.zip.InnerPrefabFile
import me.anno.io.zip.ZipCache
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.blender.BlenderReader
import me.anno.mesh.obj.OBJReader2
import me.anno.mesh.vox.VOXReader
import me.anno.utils.structures.maps.KeyPairMap
import org.apache.logging.log4j.LogManager

object PrefabCache : CacheSection("Prefab") {

    private val prefabTimeout = 60_000L
    private val LOGGER = LogManager.getLogger(PrefabCache::class)

    fun getPrefab(file: FileReference, chain: MutableSet<FileReference>? = null): Prefab? {
        return getPrefabPair(file, chain)?.prefab
    }

    private fun loadAssimpModel(resource: FileReference): Prefab? {
        return try {
            val reader = AnimatedMeshesLoader
            val meshes = reader.readAsFolder2(resource)
            meshes.second
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadVOXModel(resource: FileReference): Prefab? {
        return try {
            VOXReader().read(resource).toEntityPrefab(resource)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadObjModel(resource: FileReference): Prefab? {
        return try {
            OBJReader2(resource).scenePrefab
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadBlenderModel(resource: FileReference): Prefab? {
        return try {
            val folder = BlenderReader.readAsFolder(resource)
            val sceneFile = folder.getChild("Scene.json") as InnerPrefabFile
            return sceneFile.prefab
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun loadJson(resource: FileReference?): ISaveable? {
        return when (resource) {
            InvalidRef, null -> null
            is PrefabReadable -> resource.readPrefab()
            else -> {
                try {
                    val read = TextReader.read(resource)
                    val prefab = read.firstOrNull()
                    if (prefab == null) LOGGER.warn("No Prefab found in $resource:${resource::class.simpleName}! $read")
                    // else LOGGER.info("Read ${prefab.changes?.size} changes from $resource")
                    (prefab as? Prefab)?.wasCreatedFromJson = true
                    prefab
                } catch (e: Exception) {
                    LOGGER.warn("$e by $resource")
                    e.printStackTrace()
                    null
                }
            }
        }
    }

    private fun loadUnityFile(resource: FileReference): Prefab? {
        return loadJson(UnityReader.readAsAsset(resource)) as? Prefab
    }

    private fun loadPrefab2(resource: FileReference): ISaveable? {
        return if (resource.exists && !resource.isDirectory) {
            // LOGGER.info("resource $resource has signature $signature")
            val prefab = when (Signature.findName(resource)) {
                "vox" -> loadVOXModel(resource)
                "fbx", "gltf", "md2", "md5mesh" ->
                    loadAssimpModel(resource)
                "blend" -> loadBlenderModel(resource)
                "obj" -> loadObjModel(resource)
                "yaml" -> loadUnityFile(resource)
                "json" -> {
                    // could be gltf as well
                    when (resource.lcExtension) {
                        "gltf", "glb" -> loadAssimpModel(resource)
                        else -> loadJson(resource)
                    }
                }
                else -> {
                    when (resource.lcExtension) {
                        "vox" -> loadVOXModel(resource)
                        "fbx", "dae", "gltf", "glb",
                        "md2", "md5mesh" ->
                            loadAssimpModel(resource)
                        "blend" -> loadBlenderModel(resource)
                        "obj" -> loadObjModel(resource)
                        "unity", "mat", "prefab", "asset", "meta", "controller" -> loadUnityFile(
                            resource
                        )
                        "mtl" -> null
                        else -> loadJson(resource)
                    }
                }
            }
            (prefab as? Prefab)?.source = resource
            prefab
        } else null
    }

    private fun loadPrefab3(file: FileReference): ISaveable? {
        if (file is PrefabReadable) return file.readPrefab()
        try {
            val pure = TextReader.read(file).firstOrNull()
            if (pure != null) return pure
        } catch (e: Exception) {
        }
        val signature = Signature.findName(file)
        if (signature == "yaml") {
            try {
                val prefab = loadUnityFile(file)
                if (prefab != null) return prefab
            } catch (e: Exception) {

            }
        }
        val folder = ZipCache.unzip(file, false) ?: return null
        val scene = folder.getChild("Scene.json") as? PrefabReadable ?: return null
        return scene.readPrefab()
    }

    fun getPrefabPair(resource: FileReference?, chain: MutableSet<FileReference>?, async: Boolean = false): FileReadPrefabData? {
        resource ?: return null
        return if (resource != InvalidRef && resource.exists && !resource.isDirectory) {
            return getFileEntry(resource, false, prefabTimeout, async) { file, _ ->
                val loaded = loadPrefab3(file)
                if (loaded != null) {
                    FileWatch.addWatchDog(file)
                    FileReadPrefabData(
                        loaded as? Prefab,
                        if (loaded is Prefab) loaded.getSampleInstance(chain ?: HashSet())
                        else loaded, file
                    )
                } else CacheData(null)
            } as? FileReadPrefabData
        } else null
    }

    fun loadPrefab(resource: FileReference?, chain: MutableSet<FileReference>? = null, async: Boolean = false): Prefab? {
        return getPrefabPair(resource, chain, async)?.prefab
    }

    fun createInstance(
        prefab: Prefab?,
        superPrefab: FileReference,
        adds: List<CAdd>?,
        sets: KeyPairMap<Path, String, Any?>?,
        chain: MutableSet<FileReference>?,
        clazz: String
    ): PrefabSaveable {
        // LOGGER.info("creating instance from $superPrefab")
        val instance = createSuperInstance(superPrefab, chain, clazz)
        instance.changePaths(prefab, Path.ROOT_PATH)
        // val changes2 = (changes0 ?: emptyList()).groupBy { it.className }.map { "${it.value.size}x ${it.key}" }
        // LOGGER.info("  creating entity instance from ${changes0?.size ?: 0} changes, $changes2")
        if (adds != null) {
            for ((index, add) in adds.withIndex()) {
                try {
                    add.apply(instance, HashSet(chain))
                } catch (e: Exception) {
                    LOGGER.warn("Change $index, $add failed")
                    throw e
                }
            }
        }
        sets?.forEach { k1, k2, v ->
            try {
                CSet.apply(instance, k1, k2, v)
            } catch (e: Exception) {
                LOGGER.warn("Change '$k1' '$k2' '$v' failed")
                throw e
            }
        }
        // LOGGER.info("  created instance '${entity.name}' has ${entity.children.size} children and ${entity.components.size} components")
        return instance
    }

    fun createInstance(
        superPrefab: FileReference,
        adds: List<CAdd>?,
        sets: List<CSet>?,
        chain: MutableSet<FileReference>?,
        clazz: String
    ): PrefabSaveable {
        // LOGGER.info("creating instance from $superPrefab")
        val instance = createSuperInstance(superPrefab, chain, clazz)
        // val changes2 = (changes0 ?: emptyList()).groupBy { it.className }.map { "${it.value.size}x ${it.key}" }
        // LOGGER.info("  creating entity instance from ${changes0?.size ?: 0} changes, $changes2")
        if (adds != null) {
            for ((index, change) in adds.withIndex()) {
                try {
                    change.apply(instance, HashSet(chain))
                } catch (e: Exception) {
                    LOGGER.warn("Change $index, $change failed")
                    throw e
                }
            }
        }
        if (sets != null) {
            for (index in sets.indices) {
                val change = sets[index]
                try {
                    change.apply(instance, null)
                } catch (e: Exception) {
                    LOGGER.warn("Change $index, $change failed")
                    throw e
                }
            }
        }
        // LOGGER.info("  created instance '${entity.name}' has ${entity.children.size} children and ${entity.components.size} components")
        return instance
    }

    private fun createSuperInstance(
        prefab: FileReference,
        chain: MutableSet<FileReference>?,
        clazz: String
    ): PrefabSaveable {
        /*if (chain != null) {
            if (prefab != InvalidRef) {
                chain.add(prefab)
                if (prefab in chain) {
                    LOGGER.warn("Hit dependency ring: $chain, $prefab")
                    return ISaveable.create(clazz) as PrefabSaveable
                }
            }
        }*/
        // LOGGER.info("chain: $chain")
        return loadPrefab(prefab, chain)?.createInstance(chain) ?: ISaveable.create(clazz) as PrefabSaveable
    }

    fun loadScenePrefab(file: FileReference): Prefab {
        val prefab = loadPrefab(file, HashSet()) ?: Prefab("Entity").apply { this.prefab = ScenePrefab }
        prefab.source = file
        if (!file.exists) file.writeText(TextWriter.toText(prefab))
        return prefab
    }

}