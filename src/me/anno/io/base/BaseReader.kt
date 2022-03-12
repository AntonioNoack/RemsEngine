package me.anno.io.base

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.EOFException

abstract class BaseReader {

    private val withPtr = ArrayList<ISaveable>()
    private val withoutPtr = ArrayList<ISaveable>()

    val sortedContent: List<ISaveable> get() = (withPtr + withoutPtr).filter { it !== UnitSaveable }

    var sourceName = ""

    private val missingReferences = HashMap<Int, ArrayList<Pair<Any, String>>>()

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
                    when (obj) {
                        is ISaveable -> obj.readObject(name, value)
                        is MissingListElement -> {
                            obj.target[obj.targetIndex] = value
                        }
                        else -> throw RuntimeException("Unknown missing reference type")
                    }
                }
            }
        } else LOGGER.warn("Got object with uuid $ptr: $value, it will be ignored")
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

        private val UnitSaveable = Saveable()

        private val LOGGER = LogManager.getLogger(BaseReader::class)

        fun error(msg: String): Nothing = throw InvalidFormatException("[BaseReader] $msg")
        fun error(msg: String, appended: Any?): Nothing = throw InvalidFormatException("[BaseReader] $msg $appended")

        fun getNewClassInstance(clazz: String): ISaveable {
            // from old Rem's Studio times
            if (clazz.startsWith("AnimatedProperty<")) return getNewClassInstance("AnimatedProperty")
            return ISaveable.objectTypeRegistry[clazz]?.generate() ?: throw UnknownClassException(clazz)
        }

    }


}