package me.anno.ecs.prefab

import me.anno.cache.ICacheData
import me.anno.io.ISaveable
import me.anno.io.files.FileReference
import me.anno.io.files.FileWatch

class FileReadPrefabData(val prefab: Prefab?, val instance: ISaveable?, val reference: FileReference) :
    ICacheData {
    override fun destroy() {
        if (instance is ICacheData) instance.destroy()
        FileWatch.removeWatchDog(reference)
    }
}