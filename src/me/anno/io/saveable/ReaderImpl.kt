package me.anno.io.saveable

interface ReaderImpl {
    fun readAllInList()
    fun finish()
    val sortedContent: List<Saveable>
}