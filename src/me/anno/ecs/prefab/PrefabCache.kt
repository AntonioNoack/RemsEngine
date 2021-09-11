package me.anno.ecs.prefab

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.engine.scene.ScenePrefab
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.unity.UnityReader
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.mesh.vox.VOXReader
import org.apache.logging.log4j.LogManager

object PrefabCache : CacheSection("Prefab") {

    private val prefabTimeout = 60_000L
    private val LOGGER = LogManager.getLogger(PrefabCache::class)

    fun loadAssimpModel(resource: FileReference): Prefab? {
        return try {
            val reader = AnimatedMeshesLoader
            val meshes = reader.readAsFolder2(resource)
            meshes.second
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadVOXModel(resource: FileReference): Prefab? {
        return try {
            VOXReader().read(resource).toEntityPrefab(resource)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadObjModel(resource: FileReference): Prefab? {
        return loadAssimpModel(resource)
        /*return try {
            OBJReader2(resource).prefab
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }*/
    }

    fun loadJson(resource: FileReference?): ISaveable? {
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

    fun loadUnityFile(resource: FileReference): Prefab? {
        return loadJson(UnityReader.readAsAsset(resource)) as? Prefab
    }

    fun loadPrefab2(resource: FileReference): ISaveable? {
        return if (resource.exists && !resource.isDirectory) {
            val signature = Signature.find(resource)
            // LOGGER.info("resource $resource has signature $signature")
            val prefab = when (signature?.name) {
                "vox" -> loadVOXModel(resource)
                "fbx", "gltf",
                "md2", "md5mesh" ->
                    loadAssimpModel(resource)
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
                        "obj" -> loadObjModel(resource)
                        "unity", "mat", "prefab", "asset", "meta", "controller" -> loadUnityFile(
                            resource
                        )
                        else -> loadJson(resource)
                    }
                }
            }
            (prefab as? Prefab)?.src = resource
            prefab
        } else null
    }

    fun getPrefabPair(resource: FileReference?, async: Boolean = false): Pair<Prefab?, ISaveable>? {
        resource ?: return null
        return if (resource != InvalidRef && resource.exists && !resource.isDirectory) {
            val data = getEntry(resource, prefabTimeout, async) {
                val loaded = loadPrefab2(resource)
                CacheData(
                    when (loaded) {
                        is Prefab -> Pair(loaded, loaded.getSampleInstance())
                        is ISaveable -> Pair(null, loaded)
                        else -> null
                    }
                )
            } as? CacheData<*>
            val pair = data?.value as? Pair<*, *>
            return pair as? Pair<Prefab, PrefabSaveable>
        } else null
    }

    fun loadPrefab(resource: FileReference?): Prefab? {
        return getPrefabPair(resource)?.first
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
                /*if (chain != null && change is CAdd && change.prefab in chain) {
                    val old = change.prefab
                    change.prefab = InvalidRef
                    LOGGER.warn("Invalidated circular reference! $old in $chain")
                }*/
                try {
                    change.apply(instance)
                } catch (e: Exception) {
                    LOGGER.warn("Change $index, $change failed")
                    throw e
                }
            }
        }
        if (sets != null) {
            for ((index, change) in sets.withIndex()) {
                try {
                    change.apply(instance)
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
        return loadPrefab(prefab)?.createInstance(chain) ?: ISaveable.create(clazz) as PrefabSaveable
    }

    fun loadScenePrefab(file: FileReference): Prefab {
        val prefab = loadPrefab(file) ?: Prefab("Entity").apply { this.prefab = ScenePrefab }
        prefab.src = file
        if (!file.exists) file.writeText(TextWriter.toText(prefab))
        return prefab
    }

}