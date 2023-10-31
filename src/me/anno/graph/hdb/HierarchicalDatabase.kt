package me.anno.graph.hdb

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.graph.hdb.allocator.FileAllocation
import me.anno.graph.hdb.index.*
import me.anno.io.files.FileReference
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.studio.StudioBase.Companion.addEvent
import me.anno.utils.structures.maps.Maps.removeIf
import java.io.RandomAccessFile
import kotlin.concurrent.thread

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
) : CacheSection(name) {

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
                    IndexReader(stream) { index ->
                        synchronized(storageFiles) {
                            storageFiles.getOrPut(index) {
                                StorageFile(index)
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
                IndexWriter(stream).write(root)
            }
        }
    }

    fun cleanup(millisTimeout: Long) {
        synchronized(this) {
            val wasChanged = cleanup(System.currentTimeMillis() - millisTimeout, root)
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
                if (file.lcExtension == "bin") {
                    val index = file.nameWithoutExtension.toIntOrNull() ?: continue
                    if (index !in storageFiles || storageFiles[index]?.size == 0) {
                        file.delete()
                    }
                }
            }
        }
    }

    private fun cleanup(timeout: Long, folder: Folder): Boolean {
        var wasChangedOverall = false
        for (child in folder.children.values) {
            if (cleanup(timeout, child)) {
                wasChangedOverall = true
            }
        }
        // clean up
        val numFilesRemoved = folder.files.removeIf { (_, file) ->
            file.lastAccessedMillis < timeout
        }
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
                    val bytes = file.readBytesSync()
                    val newBytes = FileAllocation.pack(files, bytes)
                    file.writeBytes(newBytes)
                }
            }
        }
    }

    private fun getFile(sfIndex: Int): FileReference {
        return storage.getChild("$sfIndex.bin")
    }

    fun deleteAll() {
        storage.deleteRecursively()
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
                val bytes = getDataSync(sf) ?: return callback(null)
                callback(ByteSlice(bytes, file.range))
            } else {
                getDataAsync(sf) { bytes ->
                    val slice = if (bytes != null) ByteSlice(bytes, file.range) else null
                    callback(slice)
                }
            }
        }
    }

    private fun getDataSync(sf: StorageFile): ByteArray? {
        val file = getFile(sf.index)
        val data = getEntry(file, cacheTimeoutMillis, false) {
            CacheData(it.readBytesSync())
        } as? CacheData<*>
        return data?.value as? ByteArray
    }

    private fun getDataAsync(sf: StorageFile, callback: (ByteArray?) -> Unit) {
        val key = sf.index
        getEntryAsync(key, cacheTimeoutMillis, true, {
            val file = getFile(it)
            if (file.exists) {
                CacheData(file.readBytesSync())
            } else null
        }, { data, exc ->
            callback((data as? CacheData<*>)?.value as? ByteArray)
            exc?.printStackTrace()
        })
    }

    fun put(path: List<String>, hash: Long, value: ByteArray) {
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
        }

        synchronized(this) {
            addFile(hash, value, sf, folder)
        }

        scheduleIndexUpdate()
    }

    private fun addFile(hash: Long, value: ByteArray, sf: StorageFile, folder: Folder) {
        val oldData = getDataSync(sf) ?: ByteArray(0)
        if (oldData.size < sf.size) deleteBecauseCorrupted(sf)
        sf.size += value.size

        val file = File(System.currentTimeMillis(), value.indices)
        folder.files.remove(hash)
        val (_, data) = FileAllocation.insert(folder.files.values, file, value, value.size, oldData.size, oldData)
        folder.files[hash] = file

        val file1 = getFile(sf.index)
        override(file1, CacheData(data), cacheTimeoutMillis)

        if (file1.exists) {
            val writer = RandomAccessFile(file1.absolutePath, "rw")
            writer.seek(file.range.first.toLong())
            writer.write(value)
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
            for (file in storageFiles.values) {
                if (file.size + size <= targetFileSize) {
                    return file
                }
            }
            // none was found, so create a new one
            val maxOldIndex = storageFiles.values.maxOfOrNull { it.index } ?: 0
            val newFile = StorageFile(maxOldIndex + 1)
            storageFiles[newFile.index] = newFile
            return newFile
        }
    }

    private var lastUpdate = 0L
    private var needsUpdate = false
    private fun scheduleIndexUpdate() {
        runUpdateMaybe()
    }

    private fun runUpdateMaybe() {
        val time = System.nanoTime()
        if (time - lastUpdate > 1000L * MILLIS_TO_NANOS) {
            lastUpdate = time
            needsUpdate = false
            storeIndex()
        } else {
            if (needsUpdate) return
            needsUpdate = true
            thread(name = "Saving Maybe?") {
                Thread.sleep(500L)
                addEvent {
                    scheduleIndexUpdate()
                }
            }
        }
    }

    fun delete(path: List<String>, hash: Long): Boolean {
        var folder = root
        for (i in path.indices) {
            folder = folder.children[path[i]] ?: return false
        }
        return if (folder.files.remove(hash) != null) {
            optimizeStorage(folder)
            true
        } else false
    }

    companion object {
        private val B0 = ByteArray(0)
    }
}