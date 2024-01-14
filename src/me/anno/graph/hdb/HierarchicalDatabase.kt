package me.anno.graph.hdb

import me.anno.Time
import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.graph.hdb.allocator.FileAllocation
import me.anno.graph.hdb.allocator.FileAllocation.compact
import me.anno.graph.hdb.allocator.ReplaceType
import me.anno.graph.hdb.index.*
import me.anno.io.files.FileReference
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.engine.Events.addEvent
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import java.io.RandomAccessFile

/**
 * hierarchical database, where keys are paths plus hashes,
 * and values are ByteArrays
 *
 * intended for many small files, which shouldn't clobber the disk
 * */
class HierarchicalDatabase(
    name: String,
    val storage: FileReference,
    val targetFileSize: Int,
    val cacheTimeoutMillis: Long,
    deletionTimeMillis: Long,
    val dataExtension: String = "bin",
) {

    val cache = CacheSection("HDB-$name")

    private val root = Folder("")
    private val storageFiles = HashMap<Int, StorageFile>()

    private val indexFile = storage.getChild("index.json")

    init {
        loadIndex()
        cleanup(deletionTimeMillis)
    }

    fun loadIndex() {
        storage.tryMkdirs()
        if (indexFile.exists) {
            indexFile.inputStream { stream, exc ->
                if (stream != null) {
                    IndexReader(stream) { sfIndex ->
                        synchronized(storageFiles) {
                            storageFiles.getOrPut(sfIndex) {
                                StorageFile(sfIndex)
                            }
                        }
                    }.readFolder(root)
                }
                exc?.printStackTrace()
            }
        }
    }

    fun storeIndex() {
        synchronized(this) {
            indexFile.outputStream(false).use { stream ->
                IndexWriter(stream).writeFolder(root)
            }
        }
    }

    /**
     * Deletes older files than millisTimeout milliseconds.
     * If millisTimeout <= 0, nothing will get deleted.
     * */
    fun cleanup(millisTimeout: Long) {
        synchronized(this) {
            val lastValidTime = System.currentTimeMillis() - millisTimeout
            val useTimeout = millisTimeout > 0
            val wasChanged = cleanup(lastValidTime, useTimeout, root)
            cleanDirtyStorageFiles()
            if (wasChanged) {
                storeIndex()
            }
            cleanupStrayFiles()
        }
    }

    private fun cleanDirtyStorageFiles() {
        synchronized(storageFiles) {
            for ((_, sf) in storageFiles) {
                if (sf.isDirty) {
                    optimizeStorage(sf)
                }
            }
        }
    }

    private fun cleanupStrayFiles() {
        synchronized(storageFiles) {
            val files = storage.listChildren() ?: emptyList()
            for (file in files) {
                if (file == indexFile || file.isDirectory) continue
                if (file.lcExtension == dataExtension) {
                    val index = file.nameWithoutExtension.toIntOrNull() ?: continue
                    if (index !in storageFiles || storageFiles[index]?.size == 0) {
                        file.delete()
                    }
                } else file.delete()
            }
        }
    }

    private fun cleanup(lastValidTime: Long, useTimeout: Boolean, folder: Folder): Boolean {
        var wasChangedOverall = false
        for (child in folder.children.values) {
            if (cleanup(lastValidTime, useTimeout, child)) {
                wasChangedOverall = true
            }
        }
        // clean up
        val numFilesRemoved = if (useTimeout) {
            folder.files.removeIf { (_, file) ->
                file.lastAccessedMillis < lastValidTime
            }
        } else 0
        val numChildrenRemoved = folder.children.removeIf { (_, child) ->
            child.children.isEmpty() && child.files.isEmpty()
        }
        val wasChanged = numFilesRemoved > 0
        if (wasChanged) {
            folder.storageFile?.isDirty = true
        }
        if (wasChanged || numChildrenRemoved > 0) {
            wasChangedOverall = true
        }
        return wasChangedOverall
    }

    private fun optimizeStorage(folder: Folder) {
        val storageFile = folder.storageFile
        if (storageFile != null) {
            optimizeStorage(storageFile)
        }
    }

    private fun optimizeStorage(sf: StorageFile) {
        val files = sf.folders.map { it.files.values }.flatten()
        if (FileAllocation.shouldOptimize(files, sf.size)) {
            // load and manipulate storage
            synchronized(sf) {
                val file = getFile(sf.index)
                if (files.isEmpty()) {
                    file.delete()
                } else {
                    try {
                        val bytes = file.readBytesSync()
                        val newBytes = FileAllocation.pack(files, bytes)
                        file.writeBytes(newBytes)
                    } catch (e: Exception) {
                        LOGGER.warn("Lost $file", e)
                    }
                }
            }
        }
    }

    private fun getFile(sfIndex: Int): FileReference {
        return storage.getChild("$sfIndex.$dataExtension")
    }

    fun clear() {
        storage.deleteRecursively()
    }

    fun get(key: HDBKey, async: Boolean, callback: (ByteSlice?) -> Unit) {
        return get(key.path, key.hash, async, callback)
    }

    fun get(path: List<String>, hash: Long, async: Boolean, callback: (ByteSlice?) -> Unit) {
        var folder = root
        for (i in path.indices) {
            folder = folder.children[path[i]] ?: return callback(null)
        }
        val file = folder.files[hash] ?: return callback(null)
        file.lastAccessedMillis = System.currentTimeMillis()
        if (file.range.isEmpty()) {
            callback(ByteSlice(B0, file.range))
        } else {
            val sf = folder.storageFile ?: return callback(null)
            if (async) {
                getDataAsync(sf) { bytes ->
                    val slice = if (bytes != null) ByteSlice(bytes, file.range) else null
                    callback(slice)
                }
            } else {
                val bytes = getDataSync(sf) ?: return callback(null)
                callback(ByteSlice(bytes, file.range))
            }
        }
    }

    private fun loadData(sf: Int): CacheData<ByteArray>? {
        val file = getFile(sf)
        return if (file.exists) {
            CacheData(file.readBytesSync())
        } else null
    }

    private fun getDataSync(sf: StorageFile): ByteArray? {
        val key = sf.index
        val data = cache.getEntry(key, cacheTimeoutMillis, false) {
            loadData(it)
        } as? CacheData<*>
        return data?.value as? ByteArray
    }

    private fun getDataAsync(sf: StorageFile, callback: (ByteArray?) -> Unit) {
        val key = sf.index
        cache.getEntryAsync(key, cacheTimeoutMillis, true, {
            loadData(it)
        }, { data, exc ->
            callback((data as? CacheData<*>)?.value as? ByteArray)
            exc?.printStackTrace()
        })
    }

    fun put(key: HDBKey, value: ByteArray) {
        put(key.path, key.hash, ByteSlice(value))
    }

    fun put(path: List<String>, hash: Long, value: ByteSlice) {

        var folder = root
        for (i in path.indices) {
            folder = folder.children.getOrPut(path[i]) {
                Folder(path[i])
            }
        }

        var sf = folder.storageFile
        if (sf == null) {
            sf = findStorageFile(value.size)
            folder.storageFile = sf
            sf.folders.add(folder)
        }

        synchronized(this) {
            addFile(hash, value, sf, folder)
        }

        scheduleIndexUpdate()
    }

    private fun addFile(hash: Long, value: ByteSlice, sf: StorageFile, folder: Folder) {

        val oldData = getDataSync(sf) ?: B0
        if (oldData.size < sf.size) {
            LOGGER.warn("Missing data ${oldData.size} < ${sf.size}")
            deleteBecauseCorrupted(sf)
        }

        folder.files.remove(hash)
        val file = File(System.currentTimeMillis(), value.range)
        val files = sf.files
        val (type, data) = FileAllocation.insert(
            files, compact(files), file,
            value.bytes, value.range,
            oldData.size, oldData, true
        )
        folder.files[hash] = file

        val file1 = getFile(sf.index)
        sf.size = data.size

        cache.override(sf.index, CacheData(data), cacheTimeoutMillis)

        if (type == ReplaceType.InsertInto && file1.exists) {
            val writer = RandomAccessFile(file1.absolutePath, "rw")
            writer.seek(file.range.first.toLong())
            writer.write(value.bytes, value.range.first, value.size)
            writer.close()
        } else {
            storage.tryMkdirs()
            file1.writeBytes(data)
        }
    }

    private fun deleteBecauseCorrupted(sf: StorageFile) {
        synchronized(this) {
            for (folder in sf.folders) {
                folder.files.removeIf {
                    !it.value.range.isEmpty()
                }
            }
            sf.size = 0
        }
    }

    private fun findStorageFile(size: Int): StorageFile {
        // find a storage file with enough space available
        synchronized(storageFiles) {
            val maxLevel = targetFileSize - size
            if (maxLevel >= 0) {
                for (file in storageFiles.values) {
                    if (file.size <= maxLevel) {
                        return file
                    }
                }
            }
            // none was found, so create a new one
            val maxOldIndex = storageFiles.values.maxOfOrNull { it.index } ?: -1
            val newFile = StorageFile(maxOldIndex + 1)
            storageFiles[newFile.index] = newFile
            return newFile
        }
    }

    private var lastUpdate = 0L
    private var needsUpdate = false
    private fun scheduleIndexUpdate() {
        if (needsUpdate) return
        needsUpdate = true
        runIndexUpdate()
    }

    private fun runIndexUpdate() {
        if (!needsUpdate) return
        val time = Time.nanoTime
        if (time - lastUpdate > 1000L * MILLIS_TO_NANOS) {
            lastUpdate = time
            needsUpdate = false
            storeIndex()
        } else {
            addEvent(500L) {
                runIndexUpdate()
            }
        }
    }

    fun remove(key: HDBKey): Boolean {
        return remove(key.path, key.hash)
    }

    fun remove(path: List<String>, hash: Long): Boolean {
        var folder = root
        for (i in path.indices) {
            folder = folder.children[path[i]] ?: return false
        }
        return if (folder.files.remove(hash) != null) {
            optimizeStorage(folder)
            true
        } else false
    }

    fun removeAll(path: List<String>, recursive: Boolean): Boolean {
        if (recursive) TODO("remove subpaths, too?")
        var folder = root
        for (i in path.indices) {
            folder = folder.children[path[i]] ?: return false
        }
        return if (folder.files.isNotEmpty()) {
            folder.files.clear()
            optimizeStorage(folder)
            true
        } else false
    }

    companion object {
        private val B0 = ByteArray(0)
        private val LOGGER = LogManager.getLogger(HierarchicalDatabase::class)
    }
}