package me.anno.utils.bugs

object SumOf {

    // because currently Kotlin or Intellij Idea is too stupid to parse sumOf
    fun <V> sumOf(list: Iterable<V>, mapping: (V) -> Float): Float {
        var sum = 0f
        for(element in list){
            sum += mapping(element)
        }
        return sum
    }

}