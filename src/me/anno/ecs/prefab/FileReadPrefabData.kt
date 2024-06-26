package me.anno.ecs.prefab

import me.anno.cache.AsyncCacheData
import me.anno.io.saveable.Saveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch

class FileReadPrefabData(val reference: FileReference) : AsyncCacheData<Saveable>() {

    val prefab get() = value as? Prefab
    val instance
        get(): Saveable? {
            val value = value
            return if (value is Prefab) null else value
        }

    override fun destroy() {
        super.destroy()
        FileWatch.removeWatchDog(reference)
    }

    override fun toString(): String {
        return "FileReadPrefabData[${super.toString()}]"
    }
}