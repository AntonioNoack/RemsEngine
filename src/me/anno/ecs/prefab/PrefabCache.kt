package me.anno.ecs.prefab

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.cache.instances.MeshCache
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.mesh.assimp.AnimGameItem
import me.anno.mesh.assimp.AnimatedMeshesLoader
import me.anno.utils.types.Strings.getImportType

object PrefabCache : CacheSection("Prefabs") {

    val timeout = 30_000L

    fun getPrefab(path: FileReference, async: Boolean): Entity? {
        return getPrefab(path, timeout, async)
    }

    fun getPrefab(path: FileReference, timeout: Long, async: Boolean): Entity? {
        if(path == InvalidRef) return null
        val data = getEntry(path, timeout, async) { file ->
            // here goes the real importer
            val try0 = TextReader.read(file).firstOrNull() as? Entity
            val prefab: Entity = try0 ?: when (file.extension.getImportType()) {
                "Text" -> {
                    // todo just load the raw data?
                    // or do we create an ui field?
                    // it could be used for both...
                    TODO()
                }
                "Mesh" -> {
                    val reader = AnimatedMeshesLoader
                    val animGameItem: AnimGameItem = reader.load(file)
                    // todo convert it to a hierarchy, and add skeleton and animation information
                    // convert an entity into a prefab-entity
                    animGameItem.hierarchy
                }
                // todo image, audio, video
                // todo pdf documents, html, ...
                else -> TODO()
            }
            CacheData(prefab)
        } as? CacheData<*>
        return data?.value as? Entity
    }

    fun getPrefab1(path: FileReference, async: Boolean): PrefabEntity1? {
        return getPrefab1(path, timeout, async)
    }

    fun getPrefab1(path: FileReference, timeout: Long, async: Boolean): PrefabEntity1? {
        val data = getEntry(path, timeout, async) { file ->
            // here goes the real importer
            val prefab: PrefabEntity1 = when (file.extension.getImportType()) {
                "Text" -> {
                    // todo just load the raw data?
                    // or do we create an ui field?
                    // it could be used for both...
                    TODO()
                }
                "Mesh" -> {
                    val reader = AnimatedMeshesLoader
                    val animGameItem: AnimGameItem = reader.load(file)
                    // todo convert it to a hierarchy, and add skeleton and animation information
                    // convert an entity into a prefab-entity
                    convertEntityToPrefab1(animGameItem.hierarchy)
                }
                // todo image, audio, video
                // todo pdf documents, html, ...
                else -> TODO()
            }
            CacheData(prefab)
        } as? CacheData<*>
        return data?.value as? PrefabEntity1
    }

    /*fun convertEntityToPrefab(entity: Entity): PrefabEntity {
        val output = PrefabEntity1()
        output.name = entity.name
        output.description = entity.description
        output.localPosition = entity.transform.localPosition
        output.localRotation = entity.transform.localRotation
        output.localScale = entity.transform.localScale
        for (child in entity.children) {
            output.add(convertEntityToPrefab(child))
        }
        for (component in entity.components) {
            output.add(convertComponentToPrefab(component))
        }
        return output
    }

    fun convertComponentToPrefab(component: Component): PrefabComponent {
        val output = PrefabComponent()
        output.name = component.name
        output.description = component.description
        output.type = component.className
        for ((name, property) in component.getReflections().properties) {
            val value = property[component]
            // only set if changed? nah, it's fine ^^
            output.changes[name] = Change0(ChangeType0.SET_VALUE, value, 0)
        }
        return output
    }*/

    fun convertEntityToPrefab1(entity: Entity): PrefabEntity1 {
        val output = PrefabEntity1()
        output.name = entity.name
        output.description = entity.description
        output.localPosition = entity.transform.localPosition
        output.localRotation = entity.transform.localRotation
        output.localScale = entity.transform.localScale
        for (child in entity.children) {
            output.add(convertEntityToPrefab1(child))
        }
        for (component in entity.components) {
            output.add(convertComponentToPrefab1(component))
        }
        return output
    }

    fun convertComponentToPrefab1(component: Component): PrefabComponent1 {
        val output = PrefabComponent1()
        output.name = component.name
        output.description = component.description
        output.type = component.className
        for ((name, property) in component.getReflections().properties) {
            val value = property[component]
            // only set if changed? nah, it's fine ^^
            output.changes[name] = Change0(ChangeType0.SET_VALUE, value, 0)
        }
        return output
    }

}