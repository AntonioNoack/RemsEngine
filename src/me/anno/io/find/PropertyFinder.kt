package me.anno.io.find

import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.pop

/**
 * this is used within Rem's Studio to keep references to properties inside objects even after reloading
 *
 * to do support lists and their indices???
 * */
object PropertyFinder {

    fun getName(container: Saveable, searched: Saveable): String? {
        val writer = FindNameWriter(searched)
        writer.add(container)
        writer.writeAllInList()
        return writer.foundName
    }

    fun getValue(container: Saveable, searched: String): Saveable? {
        val parts = searched.split('/')
        var instance = container
        for (i in parts.indices) {
            val writer = FindValueWriter(parts[i])
            instance.save(writer)
            instance = writer.foundValue ?: return null
        }
        return instance
    }

    private class FindNameWriter(private val searched: Saveable) : PartialWriter(false) {
        var foundName: String? = null

        private val nameStack = ArrayList<String>()
        override fun writeObjectImpl(name: String?, value: Saveable) {
            if (searched === value && name != null) {
                nameStack.add(name)
                foundName = nameStack.joinToString("/")
                nameStack.pop()
            } else {
                if (name != null) nameStack.add(name)
                value.save(this)
                if (name != null) assertEquals(name, nameStack.pop())
            }
        }
    }

    private class FindValueWriter(private val searched: String) : PartialWriter(false) {

        var foundValue: Saveable? = null

        override fun writeObjectImpl(name: String?, value: Saveable) {
            if (name == searched) {
                foundValue = value
            }
        }
    }
}