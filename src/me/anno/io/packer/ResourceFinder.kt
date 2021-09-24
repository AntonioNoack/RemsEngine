package me.anno.io.packer

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache.loadPrefab
import me.anno.io.files.FileReference

object ResourceFinder {

    // todo when we drag in a resource, use Scene.json, not the file directly, so we don't need awkward loaders in the shipped game

    /**
     * find all resources, that we need to copy over
     * */
    fun findResources(prefab: Prefab, result: HashSet<FileReference>, donePrefabs: HashSet<Prefab>) {

        result.add(prefab.source)
        donePrefabs.add(prefab)

        prefab.sets.forEach { _, _, ref ->
            if (ref is FileReference && ref.exists) {
                if (result.add(ref)) {
                    // not done yet
                    val refPrefab = loadPrefab(ref)
                    if (refPrefab != null && donePrefabs.add(refPrefab)) {
                        findResources(refPrefab, result, donePrefabs)
                    }
                }
            }
        }

    }

    fun packResources(prefab: Prefab, ensurePrivacy: Boolean, dst: FileReference): FileReference {
        val resources = HashSet<FileReference>()
        findResources(prefab, resources, HashSet())
        return Packer.pack(resources.toList(), ensurePrivacy, dst, true)[prefab.source]!!
    }

}