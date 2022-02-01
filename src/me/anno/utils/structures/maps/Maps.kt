package me.anno.utils.structures.maps

object Maps {

    inline fun<K,V> MutableMap<K,V>.removeIf(filter: (Map.Entry<K,V>) -> Boolean): Int {
        if (isEmpty()) return 0
        var removeCounter = 0
        val i = entries.iterator()
        while (i.hasNext()) {
            val mapEntry = i.next()
            if (filter(mapEntry)) {
                i.remove()
                removeCounter++
            }
        }
        return removeCounter
    }

}