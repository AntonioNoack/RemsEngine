package me.anno.io.saveable

interface ReaderImpl {
    fun readAllInList()
    fun finish()
    // order can be important!
    val allInstances: List<Saveable>
}