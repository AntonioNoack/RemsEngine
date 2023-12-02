package me.anno.io.find

import me.anno.io.ISaveable
import me.anno.utils.structures.lists.Lists.pop
import kotlin.test.assertEquals

/**
 * this is used within Rem's Studio to keep references to properties inside objects even after reloading
 * */
@Suppress("unused")
object PropertyFinder {

    fun getName(container: ISaveable, searched: ISaveable): String? {
        return try {
            val writer = FindNameWriter(searched)
            writer.add(container)
            writer.writeAllInList()
            null
        } catch (e: FoundNameThrowable) {
            e.name
        }
    }

    fun getValue(container: ISaveable, searched: String): ISaveable? {
        return try {
            val writer = FindValueWriter(searched)
            writer.add(container)
            writer.writeAllInList()
            null
        } catch (e: FoundValueThrowable) {
            e.value
        }
    }

    class FoundNameThrowable(val name: String) : Throwable()
    class FoundValueThrowable(val value: ISaveable) : Throwable()

    class FindNameWriter(private val searched: Any) : PartialWriter(false) {
        private val nameStack = ArrayList<String>()
        override fun writeObjectImpl(name: String?, value: ISaveable) {
            if (searched === value && name != null) {
                nameStack.add(name)
                throw FoundNameThrowable(nameStack.joinToString("/"))
            } else {
                if (name != null) nameStack.add(name)
                value.save(this)
                if (name != null) assertEquals(name, nameStack.pop())
            }
        }
    }

    class FindValueWriter(private val searched: String) : PartialWriter(false) {
        private val nameStack = ArrayList<String>()
        override fun writeObjectImpl(name: String?, value: ISaveable) {
            if (name != null && searched.endsWith(name)) {
                nameStack.add(name)
                if (searched == nameStack.joinToString("/")) {
                    throw FoundValueThrowable(value)
                }
                assertEquals(name, nameStack.pop())
            } else {
                if (name != null) nameStack.add(name)
                value.save(this)
                if (name != null) assertEquals(name, nameStack.pop())
            }
        }
    }
}