package me.anno.cache.instances

import me.anno.cache.CacheSection
import me.anno.cache.data.ICacheData
import java.io.File

object MeshCache : CacheSection("Meshes") {
    fun getMesh(
        file: File,
        key: Any,
        timeout: Long,
        asyncGenerator: Boolean,
        generator: () -> ICacheData
    ) = getEntry(file, false, key, timeout, asyncGenerator, generator)
}