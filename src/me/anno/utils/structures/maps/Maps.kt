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

}