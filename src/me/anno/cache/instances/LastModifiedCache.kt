package me.anno.cache.instances

import me.anno.gpu.GFX.gameTime
import me.anno.io.files.FileReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object LastModifiedCache {

    data class Result(val file: FileReference, val exists: Boolean, val isDirectory: Boolean, val lastModified: Long) {

        constructor(file: FileReference, exists: Boolean) : this(
            file,
            exists,
            if (exists) file.isDirectory else false,
            if (exists) file.lastModified else 0L
        )

        constructor(file: FileReference) : this(file, file.exists)

    }

    var lastChecked = 0L
    var values = ConcurrentHashMap<FileReference, Result>()

    operator fun get(file: FileReference): Result {
        val time = gameTime
        if (abs(time - lastChecked) > timeout * 1_000_000L) {
            lastChecked = time
            values.clear()
        }
        return values.getOrPut(file) { Result(file) }
    }

    fun clear() {
        values.clear()
    }

    const val timeout = 20_000 // ms

}