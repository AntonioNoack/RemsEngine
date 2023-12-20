package me.anno.ecs.prefab

/**
 * meant for FileReferences: files that can be directly (with small delay at worst) read as a Prefab
 * */
interface PrefabReadable {
    fun readPrefab(): Prefab
}