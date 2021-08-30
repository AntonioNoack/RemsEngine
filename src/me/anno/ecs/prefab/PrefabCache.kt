package me.anno.ecs.prefab

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.io.text.TextReader
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

    fun loadJson(resource: FileReference?): Prefab? {
        return when (resource) {
            InvalidRef, null -> null
            is PrefabReadable -> resource.readPrefab()
            else -> {
                try {
                    val read = TextReader.read(resource)
                    val prefab = read.firstOrNull() as? Prefab
                    if (prefab == null) LOGGER.warn("No Prefab found in $resource:${resource::class.simpleName}! $read")
                    else LOGGER.info("Read ${prefab.changes?.size} changes from $resource")
                    prefab?.wasCreatedFromJson = true
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
        return loadJson(UnityReader.readAsAsset(resource))
    }

    fun loadPrefab2(resource: FileReference): Prefab? {
        return if (resource.exists && !resource.isDirectory) {
            val signature = Signature.find(resource)
            // LOGGER.info("resource $resource has signature $signature")
            when (signature?.name) {
                "vox" -> loadVOXModel(resource)
                "fbx", "obj", "gltf",
                "md2", "md5mesh" ->
                    loadAssimpModel(resource)
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
                        "fbx", "dae", "obj", "gltf", "glb",
                        "md2", "md5mesh" ->
                            loadAssimpModel(resource)
                        "unity", "mat", "prefab", "asset", "meta", "controller" -> loadUnityFile(
                            resource
                        )
                        else -> loadJson(resource)
                    }
                }
            }?.apply { src = resource }
        } else null
    }

    fun getPrefabPair(resource: FileReference?, async: Boolean = false): Pair<Prefab, PrefabSaveable>? {
        resource ?: return null
        return if (resource != InvalidRef && resource.exists && !resource.isDirectory) {
            val data = getEntry(resource, prefabTimeout, async) {
                val prefab = loadPrefab2(resource)
                CacheData(
                    if (prefab != null) {
                        val instance = prefab.createInstance()
                        Pair(prefab, instance)
                    } else null
                )
            } as CacheData<*>
            val pair = data.value as? Pair<*, *>
            return pair as? Pair<Prefab, PrefabSaveable>
        } else null
    }

    fun loadPrefab(resource: FileReference?): Prefab? {
        return getPrefabPair(resource)?.first
    }

}