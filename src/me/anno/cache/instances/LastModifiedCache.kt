package me.anno.cache.instances

import me.anno.gpu.GFX.gameTime
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object LastModifiedCache {

    data class Result(val file: File, val exists: Boolean, val isDirectory: Boolean, val lastModified: Long) {

        constructor(file: File, exists: Boolean) : this(
            file,
            exists,
            if (exists) file.isDirectory else false,
            if (exists) file.lastModified() else 0L
        )

        constructor(file: File) : this(file, file.exists())

        val lastAccessed = if (exists) {
            Files.readAttributes(
                file.toPath(),
                BasicFileAttributes::class.java
            )?.lastAccessTime()?.toMillis() ?: 0L
        } else 0L

        val length =
            if (isDirectory) 0L else file.length() // todo sum first level?, and add plus for additional children folders

    }

    var lastChecked = 0L
    var values = ConcurrentHashMap<File, Result>()

    fun invalidate(file: File) {
        values.remove(file)
    }

    fun invalidate(file: FileReference) {
        if (file is FileFileRef) {
            invalidate(file.file)
        }
    }

    operator fun get(file: File): Result {
        val time = gameTime
        // todo partial reload only, like a cache section, just that the entries decay
        // todo randomness in decay time
        if (abs(time - lastChecked) > timeoutNanos) {
            lastChecked = time
            values.clear()
        }
        return values.getOrPut(file) { Result(file) }
    }

    fun isDirectory(ref: FileReference): Boolean {
        return if (ref is FileFileRef) this[ref.file].exists
        else ref.isDirectory
    }

    fun exists(ref: FileReference): Boolean {
        return if (ref is FileFileRef) this[ref.file].exists
        else ref.exists
    }

    fun clear() {
        values.clear()
    }

    const val timeoutNanos = 20_000_000_000L

}