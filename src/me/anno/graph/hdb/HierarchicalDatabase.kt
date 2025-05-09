package me.anno.graph.hdb

import me.anno.Time
import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.engine.Events.addEvent
import me.anno.graph.hdb.allocator.FileAllocation
import me.anno.graph.hdb.allocator.ReplaceType
import me.anno.graph.hdb.index.File
import me.anno.graph.hdb.index.Folder
import me.anno.graph.hdb.index.IndexReader
import me.anno.graph.hdb.index.IndexWriter
import me.anno.graph.hdb.index.StorageFile
import me.anno.io.files.FileReference
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.waitFor
import me.anno.utils.async.UnitCallback
import me.anno.utils.structures.maps.Maps.removeIf
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
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
    val targetFileSize: Int = 10_000_000,
    val cacheTimeoutMillis: Long = 10_000,
    deletionTimeMillis: Long = 7 * 24 * 3600 * 1000,
    val dataExtension: String = "bin",
) {

    private val cache = CacheSection("HDB-$name")

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
                if (stream != null) loadIndex(stream)
                else exc?.printStackTrace()
            }
        }
    }

    fun loadIndex(stream: InputStream) {
        synchronized(storageFiles) {
            IndexReader(stream) { sfIndex ->
                storageFiles.getOrPut(sfIndex) {
                    StorageFile(sfIndex)
                }
            }.readFolder(root)
            // validate all storage files: files and ranges need to be reconstructed
            for ((_, sf) in storageFiles) {
                synchronized(sf) {
                    sf.rebuildSortedFiles()
                    sf.rebuildSortedRanges()
                }
            }
        }
    }

    fun storeIndex() {
        synchronized(this) {
            indexFile.outputStream(false).use { stream ->
                stream.writer().use { writer ->
                    IndexWriter(writer).writeFolder(root)
                }
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
            val files = storage.listChildren()
            for (file in files) {
                if (file == indexFile || file.isDirectory) continue
                if (file.lcExtension == dataExtension) {
                    val index = file.nameWithoutExtension.toIntOrNull()
                    if (index != null) {
                        if (index !in storageFiles || storageFiles[index]?.size == 0) {
                            file.delete()
                        }
                    } else {
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
        val files = sf.sortedFiles
        if (!FileAllocation.shouldOptimize(files, sf.size)) return
        // load and manipulate storage
        synchronized(sf) {
            val file = getFile(sf.index)
            if (files.isEmpty()) {
                file.delete()
            } else {
                file.readBytes { bytes, err ->
                    if (bytes != null) optimizeStorage(sf, file, bytes)
                    else err?.printStackTrace()
                }
            }
        }
    }

    private fun optimizeStorage(sf: StorageFile, file: FileReference, bytes: ByteArray) {
        synchronized(sf) { // we might have switched to a different thread
            val newBytes = FileAllocation.pack(sf.sortedFiles, bytes)
            file.writeBytes(newBytes)
        }
    }

    private fun getFile(sfIndex: Int): FileReference {
        return storage.getChild("$sfIndex.$dataExtension")
    }

    fun clear() {
        storage.delete()
        clearMemory()
    }

    fun clearMemory() {
        cache.clear()
        synchronized(storageFiles) {
            for (sf in storageFiles.values) {
                synchronized(sf) {
                    sf.clear()
                }
            }
        }
        loadIndex()
    }

    fun get(key: HDBKey, callback: Callback<ByteSlice>) {
        return get(key.path, key.hash, callback)
    }

    fun get(path: List<String>, hash: Long, callback: Callback<ByteSlice>) {
        var folder = root
        for (i in path.indices) {
            folder = folder.children[path[i]]
                ?: return callback.err(FileNotFoundException("Missing path '$path'"))
        }
        val file = folder.files[hash]
            ?: return callback.err(FileNotFoundException("Missing hash '$hash'"))
        file.lastAccessedMillis = System.currentTimeMillis()
        if (file.range.isEmpty()) {
            callback.ok(ByteSlice(B0, file.range))
        } else {
            val sf = folder.storageFile
                ?: return callback.err(IOException("Storage empty"))
            getDataAsync(sf, callback.map { bytes -> ByteSlice(bytes, file.range) })
        }
    }

    private fun getDataAsync(sf: StorageFile, callback: Callback<ByteArray>) {
        val key = sf.index
        cache.getEntryAsync(key, cacheTimeoutMillis, true, { keyI ->
            val file = getFile(keyI)
            val result = AsyncCacheData<ByteArray>()
            if (file.exists) file.readBytes(result)
            else result.value = null
            result
        }, callback.waitFor())
    }

    private fun getDataFromCacheOnly(sf: StorageFile): ByteArray? {
        val data = cache.getEntryWithoutGenerator(sf.index, cacheTimeoutMillis)
        return (data as? AsyncCacheData<*>)?.value as? ByteArray?
    }

    fun put(key: HDBKey, value: ByteArray, callback: UnitCallback? = null) {
        put(key, ByteSlice(value), callback)
    }

    fun put(key: HDBKey, value: ByteSlice, callback: UnitCallback? = null) {
        put(key.path, key.hash, value, callback)
    }

    private fun findFolder(path: List<String>): Folder {
        var folder = root
        for (i in path.indices) {
            folder = folder.children.getOrPut(path[i]) {
                Folder(path[i])
            }
        }
        return folder
    }

    private fun createStorageFile(folder: Folder, value: ByteSlice): StorageFile {
        val result = findStorageFile(value.size)
        folder.storageFile = result
        result.folders.add(folder)
        return result
    }

    fun put(path: List<String>, hash: Long, value: ByteSlice, callback: UnitCallback? = null) {
        val folder: Folder
        val sf: StorageFile
        synchronized(this) {
            folder = findFolder(path)
            sf = folder.storageFile ?: createStorageFile(folder, value)
        }
        getDataAsync(sf) { bytes, _ ->
            synchronized(sf) {
                addFile(hash, value, sf, folder, bytes ?: B0)
            }
            scheduleStoreIndex()
            callback?.ok(Unit)
        }
    }

    private fun addFile(hash: Long, value: ByteSlice, sf: StorageFile, folder: Folder, oldData0: ByteArray) {

        val oldData = getDataFromCacheOnly(sf) ?: oldData0
        if (oldData.size < sf.size) {
            LOGGER.warn("Missing data ${oldData.size} < ${sf.size}")
            deleteBecauseCorrupted(sf)
        }

        folder.files.remove(hash)
        val file = File(System.currentTimeMillis(), value.range)

        val (type, data) = FileAllocation.insert(
            sf.sortedFiles, sf.sortedRanges, file,
            value.bytes, value.range,
            oldData.size, oldData, true
        )
        folder.files[hash] = file

        val file1 = getFile(sf.index)
        sf.size = data.size

        val forCache = AsyncCacheData<ByteArray>()
        forCache.value = data
        cache.override(sf.index, forCache, cacheTimeoutMillis)

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
        synchronized(sf) {
            for (folder in sf.folders) {
                folder.files.removeIf { (_, file) -> !file.range.isEmpty() }
            }
            sf.sortedFiles.clear()
            sf.sortedRanges.clear()
            sf.size = 0
        }
    }

    private fun findStorageFile(size: Int): StorageFile {
        // find a storage file with enough space available
        synchronized(storageFiles) {
            val maxFillSize = targetFileSize - size
            if (maxFillSize >= 0) {
                val validFile = storageFiles.values.firstOrNull { it.size <= maxFillSize }
                if (validFile != null) return validFile
            }
            // none was found, so create a new one
            return createNewStorageFile()
        }
    }

    private fun createNewStorageFile(): StorageFile {
        val maxOldIndex = storageFiles.values.maxOfOrNull { it.index } ?: -1
        val newFile = StorageFile(maxOldIndex + 1)
        storageFiles[newFile.index] = newFile
        return newFile
    }

    private var lastUpdate = 0L
    private var needsUpdate = false
    private fun scheduleStoreIndex() {
        if (needsUpdate) return
        needsUpdate = true
        storeIndexMaybe()
    }

    fun storeIndexMaybe() {
        if (!needsUpdate) return
        val time = Time.nanoTime
        if (time - lastUpdate > 1000L * MILLIS_TO_NANOS) {
            lastUpdate = time
            needsUpdate = false
            storeIndex()
        } else {
            addEvent(500L) {
                storeIndexMaybe()
            }
        }
    }

    fun remove(key: HDBKey): Boolean {
        return remove(key.path, key.hash)
    }

    fun remove(path: List<String>, hash: Long): Boolean {
        synchronized(this) {
            var folder = root
            for (i in path.indices) {
                folder = folder.children[path[i]] ?: return false
            }
            return if (folder.files.remove(hash) != null) {
                optimizeStorage(folder)
                true
            } else false
        }
    }

    fun removeAll(path: List<String>, recursive: Boolean): Boolean {
        if (recursive) TODO("remove subpaths, too?")
        synchronized(this) {
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
    }

    companion object {
        private val B0 = ByteArray(0)
        private val LOGGER = LogManager.getLogger(HierarchicalDatabase::class)
    }
}