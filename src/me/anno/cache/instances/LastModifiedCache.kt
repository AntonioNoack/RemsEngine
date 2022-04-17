package me.anno.cache.instances

import me.anno.Engine.gameTime
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.maths.Maths.MILLIS_TO_NANOS
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

        val length = if (isDirectory) 0L else file.length()

    }

    var lastChecked = 0L
    var values = ConcurrentHashMap<String, Result>()

    fun invalidate(absolutePath: String) {
        // we store both variants
        values.remove(absolutePath.replace('/', '\\'))
        values.remove(absolutePath.replace('\\', '/'))
    }

    fun invalidate(file: File) {
        invalidate(file.absolutePath)
    }

    fun invalidate(file: FileReference) {
        if (file is FileFileRef) {
            invalidate(file.file.absolutePath)
        }
    }

    fun update() {
        val time = gameTime
        // todo partial reload only, like a cache section, just that the entries decay
        // todo randomness in decay time
        if (abs(time - lastChecked) > timeoutNanos) {
            lastChecked = time
            values.clear()
        }
    }

    operator fun get(file: File, absolutePath: String): Result {
        update()
        return values.getOrPut(absolutePath) {
            val r = Result(file)
            values[absolutePath.replace('/', '\\')] = r
            values[absolutePath.replace('\\', '/')] = r
            r
        }
    }

    operator fun get(absolutePath: String): Result {
        update()
        return values.getOrPut(absolutePath) {
            val r = Result(File(absolutePath))
            values[absolutePath.replace('/', '\\')] = r
            values[absolutePath.replace('\\', '/')] = r
            r
        }
    }

    operator fun get(file: File): Result = get(file, file.absolutePath)

    fun isDirectory(ref: FileReference): Boolean {
        return if (ref is FileFileRef) this[ref.file.absolutePath].exists
        else ref.isDirectory
    }

    fun exists(ref: FileReference): Boolean {
        return if (ref is FileFileRef) this[ref.file.absolutePath].exists
        else ref.exists
    }

    fun exists(file: File): Boolean {
        return this[file, file.absolutePath].exists
    }

    fun exists(absolutePath: String): Boolean {
        return this[absolutePath].exists
    }

    fun clear() {
        values.clear()
    }

    private const val timeoutNanos = 20_000L * MILLIS_TO_NANOS
    // private val LOGGER = LogManager.getLogger(LastModifiedCache::class)

}