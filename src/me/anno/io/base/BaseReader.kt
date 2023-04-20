package me.anno.io.base

import me.anno.Build
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.EOFException

abstract class BaseReader {

    private val withPtr = ArrayList<ISaveable>()
    private val withoutPtr = ArrayList<ISaveable>()

    val allInstances = ArrayList<ISaveable>()

    val sortedContent: List<ISaveable> get() = (withPtr + withoutPtr).filter { it !== UnitSaveable }

    // for debugging
    var sourceName = ""

    private val missingReferences = HashMap<Int, ArrayList<Pair<ISaveable, String>>>()

    fun getByPointer(ptr: Int, warnIfMissing: Boolean): ISaveable? {
        val index = ptr - 1
        when {
            index in withPtr.indices -> return withPtr[index]
            warnIfMissing -> {
                if (sourceName.isBlank2()) {
                    LOGGER.warn("Missing object *$ptr, only ${withPtr.size} available")
                } else {
                    LOGGER.warn("Missing object *$ptr, only ${withPtr.size} available by '$sourceName'")
                }
            }
        }
        return null
    }

    private fun setContent(ptr: Int, iSaveable: ISaveable) {
        // LOGGER.info("SetContent($ptr, ${iSaveable.className})")
        if (ptr < 0) withoutPtr.add(iSaveable)
        else {
            // add missing instances
            val index = ptr - 1
            for (i in withPtr.size..index) {
                withPtr.add(UnitSaveable)
            }
            withPtr[index] = iSaveable
        }
    }

    fun register(value: ISaveable, ptr: Int) {
        if (ptr != 0) {
            setContent(ptr, value)
            val missingReferences = missingReferences[ptr]
            if (missingReferences != null) {
                for ((obj, name) in missingReferences) {
                    obj.readObject(name, value)
                }
            }
        } else LOGGER.warn("Got object with uuid 0: $value, it will be ignored")
    }

    fun addMissingReference(owner: ISaveable, name: String, childPtr: Int) {
        missingReferences
            .getOrPut(childPtr) { ArrayList() }
            .add(owner to name)
    }

    open fun assert(b: Boolean) {
        if (!b) throw InvalidFormatException("Assertion failed")
    }

    open fun assert(b: Boolean, msg: String) {
        if (!b) throw InvalidFormatException(msg)
    }

    open fun assert(isValue: String, shallValue: String) {
        if (!isValue.equals(shallValue, true)) {
            throw InvalidFormatException("Expected $shallValue but got $isValue")
        }
    }

    open fun assert(isValue: Char, shallValue: Char) {
        if (isValue == (-1).toChar()) throw EOFException()
        if (isValue != shallValue.lowercaseChar() && isValue != shallValue.uppercaseChar()) {
            throw InvalidFormatException("Expected $shallValue but got $isValue, ${isValue.code}")
        }
    }

    open fun assert(isValue: Char, shallValue: Char, context: String) {
        if (isValue == (-1).toChar()) throw EOFException()
        if (isValue != shallValue.lowercaseChar() && isValue != shallValue.uppercaseChar()) {
            throw InvalidFormatException("Expected $shallValue but got $isValue for $context")
        }
    }

    fun start(): Int = allInstances.size
    fun finish(start: Int = 0) {
        for (i in start until allInstances.size) {
            allInstances[i].onReadingEnded()
        }
    }

    abstract fun readObject(): ISaveable
    abstract fun readAllInList()

    companion object {

        private val UnitSaveable = Saveable()

        private val LOGGER = LogManager.getLogger(BaseReader::class)

        fun error(msg: String): Nothing = throw InvalidFormatException("[BaseReader] $msg")
        fun error(msg: String, appended: Any?): Nothing = throw InvalidFormatException("[BaseReader] $msg $appended")

        fun getNewClassInstance(className: String): ISaveable {
            val type = ISaveable.objectTypeRegistry[className]
            if (type == null) {
                if (Build.isDebug) debugInfo(className)
                throw UnknownClassException(className)
            }
            val instance = type.generate()!!
            instance.onReadingStarted()
            return instance
        }

        fun debugInfo(className: String) {
            println(
                "Looking for $className:${className.hashCode()}, " +
                        "available: ${ISaveable.objectTypeRegistry.keys.joinToString { "${it}:${it.hashCode()}:${if (it == className) 1 else 0}" }}"
            )
        }

    }

}