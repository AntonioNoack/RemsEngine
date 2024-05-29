package me.anno.io.files

import me.anno.Time
import me.anno.cache.CacheSection
import me.anno.maths.Maths
import me.anno.utils.structures.maps.Maps.removeIf
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

object LastModifiedCache {

    class Result(
        val file: File, val exists: Boolean,
        val isDirectory: Boolean, val lastModified: Long
    ) {

        var lastChecked = 0L

        constructor(file: File, exists: Boolean) : this(
            file,
            exists,
            if (exists) file.isDirectory else false,
            if (exists) file.lastModified() else 0L
        )

        constructor(file: File) : this(file, file.exists())

        val lastAccessed: Long
        val creationTime: Long

        init {
            if (exists) {
                val attr = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java) ?: null
                lastAccessed = attr?.lastAccessTime()?.toMillis() ?: 0L
                creationTime = attr?.creationTime()?.toMillis() ?: 0L
            } else {
                lastAccessed = 0L
                creationTime = 0L
            }
        }

        val length = if (isDirectory) 0L else file.length()
    }

    val values: MutableMap<String, Result> = ConcurrentHashMap()

    fun invalidate(absolutePath: String) {
        // we store both variants
        val p0 = absolutePath.replace('/', '\\')
        val p1 = absolutePath.replace('\\', '/')
        values.removeIf { (key) -> key.startsWith(p0) || key.startsWith(p1) }
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
        values.removeIf { (_, value) ->
            Time.gameTimeN - value.lastChecked > timeoutNanos
            false
        }
    }

    operator fun get(file: File, absolutePath: String): Result {
        return values.getOrPut(absolutePath) {
            val r = Result(file)
            // randomness for random decay: from 0.75x to 1.5x
            r.lastChecked = Time.gameTimeN + ((196 + (Maths.random() * 196).toInt()) * timeoutNanos ushr 8)
            values[absolutePath.replace('/', '\\')] = r
            values[absolutePath.replace('\\', '/')] = r
            r
        }
    }

    operator fun get(absolutePath: String): Result {
        return values.getOrPut(absolutePath) {
            val file = File(absolutePath)
            val exists = file.exists()
            val dir = if (exists) file.isDirectory else false
            val lm = if (exists) file.lastModified() else 0L
            val result = Result(file, exists, dir, lm)
            if ('/' in absolutePath) values[absolutePath.replace('/', '\\')] = result
            if ('\\' in absolutePath) values[absolutePath.replace('\\', '/')] = result
            result
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

    private var timeoutNanos = 20_000L * Maths.MILLIS_TO_NANOS

    init {
        CacheSection.registerOnUpdate(::update)
    }
}