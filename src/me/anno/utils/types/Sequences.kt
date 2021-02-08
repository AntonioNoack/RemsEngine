package me.anno.utils.types

object Sequences {
    fun <V> Sequence<V>.getOrNull(index: Int): V? {
        val iterator = iterator()
        var i = index
        while(i > 0 && iterator.hasNext()){
            iterator.next()
            i--
        }
        return if(iterator.hasNext()) iterator.next()
        else null
    }
}