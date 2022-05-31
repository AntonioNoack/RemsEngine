package me.anno.utils.bugs

object SumOf {

    // because currently Kotlin or Intellij Idea is too stupid to parse sumOf
    // or there is no float variant
    fun <V> sumOf(iterable: Iterable<V>, mapping: (V) -> Float): Float {
        var sum = 0f
        if (iterable is List<V>) {
            for (index in iterable.indices) {
                sum += mapping(iterable[index])
            }
        } else {
            for (element in iterable) {
                sum += mapping(element)
            }
        }
        return sum
    }

}