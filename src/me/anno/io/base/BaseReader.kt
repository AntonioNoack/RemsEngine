package me.anno.io.base

import me.anno.Build
import me.anno.io.Saveable
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.IOException

abstract class BaseReader {

    private val withPtr = ArrayList<Saveable>()
    private val withoutPtr = ArrayList<Saveable>()

    val allInstances = ArrayList<Saveable>()

    val sortedContent: List<Saveable> get() = (withPtr + withoutPtr).filter { it !== UnitSaveable }

    // for debugging
    var sourceName = ""

    private val missingReferences = HashMap<Int, ArrayList<Pair<Saveable, String>>>()

    fun getByPointer(ptr: Int, warnIfMissing: Boolean): Saveable? {
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

    private fun setContent(ptr: Int, Saveable: Saveable) {
        // LOGGER.info("SetContent($ptr, ${Saveable.className})")
        if (ptr < 0) withoutPtr.add(Saveable)
        else {
            // add missing instances
            val index = ptr - 1
            for (i in withPtr.size..index) {
                withPtr.add(UnitSaveable)
            }
            withPtr[index] = Saveable
        }
    }

    fun register(value: Saveable, ptr: Int) {
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

    fun addMissingReference(owner: Saveable, name: String, childPtr: Int) {
        missingReferences
            .getOrPut(childPtr) { ArrayList() }
            .add(owner to name)
    }

    fun start(): Int = allInstances.size
    fun finish(start: Int = 0) {
        for (i in start until allInstances.size) {
            allInstances[i].onReadingEnded()
        }
    }

    abstract fun readObject(): Saveable
    abstract fun readAllInList()

    companion object {

        private val UnitSaveable = Saveable()

        private val LOGGER = LogManager.getLogger(BaseReader::class)

        fun <V> assertEquals(a: V, b: V, msg: String) {
            if (a != b) throw IOException("$msg, $a != $b")
        }

        fun <V> assertEquals(a: V, b: V) {
            if (a != b) throw IOException("$a != $b")
        }

        fun error(msg: String): Nothing = throw InvalidFormatException("[BaseReader] $msg")
        fun error(msg: String, appended: Any?): Nothing = throw InvalidFormatException("[BaseReader] $msg $appended")

        fun getNewClassInstance(className: String): Saveable {
            val type = Saveable.objectTypeRegistry[className]
            if (type == null) {
                if (Build.isDebug) debugInfo(className)
                throw UnknownClassException(className)
            }
            val instance = type.generate()
            instance.onReadingStarted()
            return instance
        }

        fun debugInfo(className: String) {
            LOGGER.info(
                "Looking for $className:${className.hashCode()}, " +
                        "available: ${Saveable.objectTypeRegistry.keys.joinToString { "${it}:${it.hashCode()}:${if (it == className) 1 else 0}" }}"
            )
        }
    }
}