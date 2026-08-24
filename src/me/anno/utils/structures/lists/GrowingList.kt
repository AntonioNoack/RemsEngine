package me.anno.utils.structures.lists

/**
 * create a list, where the list gets extended as needed
 * */
class GrowingList<V>(val generator: (Int) -> V) : ArrayList<V>() {
    override fun get(index: Int): V {
        while (index >= size) add(generator(size))
        return super.get(index)
    }
}