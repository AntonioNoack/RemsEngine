package me.anno.utils.structures.lists

class ResetArrayList<V>(cap: Int = 16): SimpleList<V>() {

    override var size = 0

    private val content = ArrayList<V>(cap)

    fun resize(newSize: Int) {
        if (newSize > content.size) {
            content.subList(newSize, content.size).clear()
            content.trimToSize()
        }
    }

    fun add(element: V) {
        if (size >= content.size) content.add(element)
        else content[size] = element
        size++
    }

    override operator fun get(index: Int): V = content[index]
}