package me.anno.cache

import me.anno.io.files.FileReference
import me.anno.io.files.LinkFileReference

/**
 * Reduces cache lookups by storing the async value
 * */
class FileCacheValue<V : Any>(
    file: FileReference,
    val cache: (FileReference) -> Promise<V>
) {

    private var cachedValue: Promise<V>? = null
    private var lastModified: Long = 0L

    var file: FileReference = file
        set(value) {
            val oldField = field
            field = value // must be here!
            if (oldField != value) {
                invalidate()
            }
        }

    val value: V?
        get() = waitFor().value

    fun waitFor(): Promise<V> {

        val file = file
        val cachedValue = cachedValue
        if (cachedValue != null &&
            !cachedValue.hasBeenDestroyed &&
            file.lastModified == lastModified
        ) {

            // if file was already resolved, but is a link, make it direct
            if (file is LinkFileReference) {
                val resolved = file.originalOrNull
                if (resolved != null) {
                    this.file = resolved
                    this.lastModified = resolved.lastModified
                    this.cachedValue = cachedValue
                }
            }

            cachedValue.update(10_000L)
            return cachedValue
        }

        val value = cache(file) // should not be synchronized to prevent deadlocks
        synchronized(this) {
            lastModified = file.lastModified
            this.cachedValue = value
        }
        return value
    }

    private fun invalidate() {
        synchronized(this) {
            lastModified = 0L
            cachedValue = null
        }
    }
}