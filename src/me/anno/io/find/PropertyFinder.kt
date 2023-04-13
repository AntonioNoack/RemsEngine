package me.anno.io.find

import me.anno.io.ISaveable

/**
 * this is used within Rem's Studio to find, which pointers are referenced within a safe file
 * (to keep the file slightly smaller and more readable)
 * */
@Suppress("unused")
object PropertyFinder {

    fun getName(container: ISaveable, searched: ISaveable): String? {

        try {
            val writer = FindNameWriter(searched)
            writer.add(container)
            writer.writeAllInList()
        } catch (e: FoundNameThrowable) {
            return e.name
        }

        return null

    }

    fun getValue(container: ISaveable, searched: String): ISaveable? {

        try {
            val writer = FindValueWriter(searched)
            writer.add(container)
            writer.writeAllInList()
        } catch (e: FoundValueThrowable) {
            return e.value
        }

        return null

    }

    class FoundNameThrowable(val name: String) : Throwable()
    class FoundValueThrowable(val value: ISaveable) : Throwable()

    class FindNameWriter(private val searched: Any) : PartialWriter(false) {
        override fun writeObjectImpl(name: String?, value: ISaveable) {
            if(searched === value && name != null){
                throw FoundNameThrowable(name)
            } else value.save(this)
        }
    }

    class FindValueWriter(private val searched: String) : PartialWriter(false) {
        override fun writeObjectImpl(name: String?, value: ISaveable) {
            if(searched == name){
                throw FoundValueThrowable(value)
            } else value.save(this)
        }
    }

}