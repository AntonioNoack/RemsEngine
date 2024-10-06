package me.anno.io.base

import me.anno.io.saveable.ReaderImpl
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.UnknownSaveable
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import java.io.IOException

abstract class BaseReader : ReaderImpl {

    private val byPointer = HashMap<Int, Saveable>()
    override val allInstances = ArrayList<Saveable>()

    // for debugging
    var sourceName = ""

    private val missingReferences = HashMap<Int, ArrayList<Pair<Saveable, String>>>()

    fun getByPointer(ptr: Int, warnIfMissing: Boolean): Saveable? {
        val instance = byPointer[ptr]
        if (instance == null && warnIfMissing) {
            if (sourceName.isBlank2()) {
                LOGGER.warn("Missing object *$ptr, only ${byPointer.size} available")
            } else {
                LOGGER.warn("Missing object *$ptr, only ${byPointer.size} available by '$sourceName'")
            }
        }
        return instance
    }

    fun register(value: Saveable, ptr: Int) {
        if (ptr != 0) {
            byPointer[ptr] = value
            val missingReferences = missingReferences[ptr]
            if (missingReferences != null) {
                for ((obj, name) in missingReferences) {
                    obj.setProperty(name, value)
                }
            }
        } else LOGGER.warn("Got object with uuid 0: $value, it will be ignored")
    }

    fun addMissingReference(owner: Saveable, name: String, childPtr: Int) {
        missingReferences
            .getOrPut(childPtr, ::ArrayList)
            .add(owner to name)
    }

    override fun finish() {
        for (i in 0 until allInstances.size) {
            allInstances[i].onReadingEnded()
        }
    }

    abstract fun readObject(): Saveable

    companion object {

        private val UnitSaveable = Saveable()

        private val LOGGER = LogManager.getLogger(BaseReader::class)

        fun <V> assertEquals(a: V, b: V, msg: String) {
            if (a != b) throw IOException("$msg, $a != $b")
        }

        fun <V> assertEquals(a: V, b: V) {
            if (a != b) throw IOException("$a != $b")
        }

        fun getNewClassInstance(className: String): Saveable {
            val type = Saveable.objectTypeRegistry[className]
            val instance = if (type == null) {
                LOGGER.warn("Missing class $className, using UnknownSaveable instead")
                val instance = UnknownSaveable()
                instance.className = className
                instance
            } else type.generate()
            instance.onReadingStarted()
            return instance
        }
    }
}