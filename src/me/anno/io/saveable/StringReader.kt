package me.anno.io.saveable

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass

/**
 * reads objects from strings
 * */
interface StringReader {

    companion object {
        private val LOGGER = LogManager.getLogger(StringReader::class)
    }

    fun createReader(data: CharSequence, workspace: FileReference, sourceName: String): ReaderImpl

    /**
     * parses a string into an object
     * @param safely return current results on failure, else throws Exception
     * */
    fun read(data: CharSequence, workspace: FileReference, safely: Boolean): List<Saveable> {
        return read(data, workspace, "", safely)
    }

    /**
     * parses a string into an object
     * @param safely return current results on failure, else throws Exception
     * */
    fun read(data: CharSequence, workspace: FileReference, sourceName: String, safely: Boolean): List<Saveable> {
        val reader = createReader(data, workspace, sourceName)
        if (safely) {
            try {
                reader.readAllInList()
            } catch (e: Exception) {
                LOGGER.warn("Error in $sourceName", e)
            }
        } else {
            reader.readAllInList()
        }
        reader.finish()
        // sorting is very important
        return reader.sortedContent
    }

    fun toText(element: Saveable, workspace: FileReference): String

    fun <Type : Saveable> readFirstOrNull(
        data: String, workspace: FileReference,
        clazz: KClass<Type>,
        safely: Boolean = true
    ): Type? {
        return read(data, workspace, safely)
            .firstInstanceOrNull2(clazz)
    }

    fun <Type : Saveable> readFirst(
        data: String, workspace: FileReference,
        clazz: KClass<Type>, safely: Boolean = true
    ): Type {
        return readFirstOrNull(data, workspace, clazz, safely)!!
    }

    fun <V : Saveable> clone(element: V): V {
        val clone = read(toText(element, InvalidRef), InvalidRef, true).getOrNull(0)
        @Suppress("unchecked_cast")
        return clone as V
    }
}
