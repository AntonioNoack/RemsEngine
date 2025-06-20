package me.anno.io.saveable

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import org.apache.logging.log4j.LogManager
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

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
        return reader.allInstances
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
        val workspace = InvalidRef
        val serialized = toText(element, workspace)
        val cloned = read(serialized, workspace, true).getOrNull(0)
        return element::class.safeCast(cloned)!!
    }
}
