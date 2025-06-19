package me.anno.ecs.prefab

import me.anno.cache.ICacheData
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch
import me.anno.io.saveable.Saveable

class PrefabPair(val reference: FileReference) : ICacheData {

    var value: Saveable? = null
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