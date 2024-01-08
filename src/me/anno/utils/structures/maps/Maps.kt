package me.anno.utils.structures.maps

object Maps {

    @JvmStatic
    inline fun <K, V> MutableMap<K, V>.removeIf(filter: (Map.Entry<K, V>) -> Boolean): Int {
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
    @Suppress("unused")
    fun Map<Int, Int>.flatten(default: Int): IntArray {
        val maxIndex = keys.maxOrNull() ?: 0
        val array = IntArray(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[key] = value
        }
        return array
    }

    @JvmStatic
    fun <K> Map<K, Int>.flatten(default: Int, keyMapper: (K) -> Int): IntArray {
        val maxIndex = keys.maxOfOrNull { keyMapper(it) } ?: 0
        val array = IntArray(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[keyMapper(key)] = value
        }
        return array
    }

    @JvmStatic
    @Suppress("unused")
    inline fun <reified V> Map<Int, V>.flatten(default: V): Array<V> {
        val maxIndex = keys.maxOrNull() ?: 0
        val array = Array(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[key] = value
        }
        return array
    }

    @JvmStatic
    @Suppress("unused")
    inline fun <reified V, K> Map<K, V>.flatten(default: V, keyMapper: (K) -> Int): Array<V> {
        val maxIndex = keys.maxOfOrNull { keyMapper(it) } ?: 0
        val array = Array(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[keyMapper(key)] = value
        }
        return array
    }

}