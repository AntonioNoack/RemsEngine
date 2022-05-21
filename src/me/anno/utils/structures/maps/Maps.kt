package me.anno.utils.structures.maps

object Maps {

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

    /**
     * removeIf without .iterator()
     * */
    @Suppress("JavaMapForEach")
    inline fun <K, V> HashMap<K, V>.removeIf2(crossinline filter: (K, V) -> Boolean): Int {
        if (isEmpty()) return 0
        var toRemove: ArrayList<K>? = null
        forEach { k, v ->
            if (filter(k, v)) {
                if (toRemove == null) toRemove = ArrayList()
                toRemove!!.add(k)
            }
        }
        return if (toRemove != null && toRemove!!.isNotEmpty()) {
            val tr = toRemove!!
            for (index in tr.indices) {
                remove(tr[index])
            }
            tr.size
        } else 0
    }

    fun Map<Int, Int>.flatten(default: Int): IntArray {
        val maxIndex = keys.maxOrNull() ?: 0
        val array = IntArray(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[key] = value
        }
        return array
    }

    fun <K> Map<K, Int>.flatten(default: Int, keyMapper: (K) -> Int): IntArray {
        val maxIndex = keys.maxOfOrNull { keyMapper(it) } ?: 0
        val array = IntArray(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[keyMapper(key)] = value
        }
        return array
    }

    inline fun <reified V> Map<Int, V>.flatten(default: V): Array<V> {
        val maxIndex = keys.maxOrNull() ?: 0
        val array = Array(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[key] = value
        }
        return array
    }

    inline fun <reified V, K> Map<K, V>.flatten(default: V, keyMapper: (K) -> Int): Array<V> {
        val maxIndex = keys.maxOfOrNull { keyMapper(it) } ?: 0
        val array = Array(maxIndex + 1) { default }
        for ((key, value) in this) {
            array[keyMapper(key)] = value
        }
        return array
    }

}