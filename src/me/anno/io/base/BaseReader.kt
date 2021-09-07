package me.anno.io.base

import me.anno.io.ISaveable
import me.anno.io.InvalidFormatException
import me.anno.io.Saveable
import org.apache.logging.log4j.LogManager
import java.io.EOFException

abstract class BaseReader {

    private val content2 = ArrayList<ISaveable>()
    private val content3 = ArrayList<ISaveable>()

    val sortedContent: List<ISaveable> get() = content2 + content3

    private val missingReferences = HashMap<Int, ArrayList<Pair<Any, String>>>()

    fun getByPointer(ptr: Int): ISaveable? {
        return content2.getOrNull(ptr - 1)
    }

    private fun setContent(ptr: Int, iSaveable: ISaveable) {
        if (ptr < 0) content3.add(iSaveable)
        else {
            // add missing instances
            val index = ptr - 1
            for (i in content2.size..index) {
                content2.add(UnitSaveable)
            }
            content2[index] = iSaveable
        }
    }

    fun register(value: ISaveable, ptr: Int) {
        if (ptr != 0) {
            setContent(ptr, value)
            val missingReferences = missingReferences[ptr]
            if (missingReferences != null) {
                for ((obj, name) in missingReferences) {
                    when (obj) {
                        is ISaveable -> obj.readObject(name, value)
                        is MissingListElement -> {
                            obj.target[obj.targetIndex] = value
                        }
                        else -> throw RuntimeException("Unknown missing reference type")
                    }
                }
            }
        } else if (ptr == 0) LOGGER.warn("Got object with uuid $ptr: $value, it will be ignored")
    }

    fun addMissingReference(owner: Any, name: String, childPtr: Int) {
        missingReferences
            .getOrPut(childPtr) { ArrayList() }
            .add(owner to name)
    }

    fun assert(b: Boolean) {
        if (!b) throw InvalidFormatException("Assertion failed")
    }

    fun assert(b: Boolean, msg: String) {
        if (!b) throw InvalidFormatException(msg)
    }

    fun assert(isValue: String, shallValue: String) {
        if (!isValue.equals(shallValue, true)) {
            throw InvalidFormatException("Expected $shallValue but got $isValue")
        }
    }

    fun assert(isValue: Char, shallValue: Char) {
        if (isValue == (-1).toChar()) throw EOFException()
        if (isValue != shallValue.lowercaseChar() && isValue != shallValue.uppercaseChar()) {
            throw InvalidFormatException("Expected $shallValue but got $isValue, ${isValue.code}")
        }
    }

    fun assert(isValue: Char, shallValue: Char, context: String) {
        if (isValue == (-1).toChar()) throw EOFException()
        if (isValue != shallValue.lowercaseChar() && isValue != shallValue.uppercaseChar()) {
            throw InvalidFormatException("Expected $shallValue but got $isValue for $context")
        }
    }

    abstract fun readObject(): ISaveable
    abstract fun readAllInList()

    companion object {

        private val UnitSaveable = object : Saveable() {}

        private val LOGGER = LogManager.getLogger(BaseReader::class)

        fun error(msg: String): Nothing = throw InvalidFormatException("[BaseReader] $msg")
        fun error(msg: String, appended: Any?): Nothing = throw InvalidFormatException("[BaseReader] $msg $appended")

        fun getNewClassInstance(clazz: String): ISaveable {
            return ISaveable.objectTypeRegistry[clazz]?.generate() ?: throw UnknownClassException(clazz)
        }
    }


}