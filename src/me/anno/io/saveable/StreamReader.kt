package me.anno.io.saveable

import me.anno.cache.AsyncCacheData
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import kotlin.reflect.KClass

/**
 * reads objects from input streams
 * */
interface StreamReader {

    companion object {
        private val LOGGER = LogManager.getLogger(StreamReader::class)
    }

    fun createReader(data: InputStream, workspace: FileReference, sourceName: String): ReaderImpl

    fun read(data: InputStream, workspace: FileReference, sourceName: String, safely: Boolean): List<Saveable> {
        val reader = createReader(data, workspace, sourceName)
        if (safely) {
            try {
                reader.readAllInList()
            } catch (e: Exception) {
                LOGGER.warn("Error in $sourceName", e)
            }
        } else reader.readAllInList()
        reader.finish()
        return reader.allInstances
    }

    fun read(file: FileReference, workspace: FileReference, safely: Boolean, result: AsyncCacheData<List<Saveable>>) {
        file.inputStream(result.map { input ->
            read(input, workspace, file.absolutePath, safely)
        })
    }

    fun read(data: InputStream, workspace: FileReference, safely: Boolean): List<Saveable> {
        return read(data, workspace, "", safely)
    }

    fun <Type : Saveable> readFirstOrNull(
        data: FileReference, workspace: FileReference,
        clazz: KClass<Type>, safely: Boolean,
        result: AsyncCacheData<Type>
    ) {
        val tmp = AsyncCacheData<List<Saveable>>()
        read(data, workspace, safely, tmp)
        tmp.waitFor { result.value = it?.firstInstanceOrNull2(clazz) }
    }
}