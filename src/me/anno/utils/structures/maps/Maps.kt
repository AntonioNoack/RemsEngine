package me.anno.utils.structures.maps

object Maps {

    @JvmStatic
    fun <K, V> MutableMap<K, V>.removeIf(filter: (Map.Entry<K, V>) -> Boolean): Int {
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

    @JvmStatic
    fun Map<Int, Int>.flatten(default: Int): IntArray {
        val maxIndex = keys.maxOrNull() ?: 0
        val array = IntArray(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[key] = value
        }
        return array
    }
}