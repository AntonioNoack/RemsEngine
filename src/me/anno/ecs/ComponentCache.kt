package me.anno.ecs

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader

object ComponentCache : CacheSection("Components") {

    fun get(reference: FileReference): Component? {
        return if (reference != InvalidRef) {
            val cacheData = getEntry(reference, 1000, false) {
                // load the component from source
                CacheData(TextReader.fromText(it.readText()) as Component)
            } as? CacheData<*>
            cacheData?.value as? Component
        } else null
    }

}