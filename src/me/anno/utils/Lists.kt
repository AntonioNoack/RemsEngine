package me.anno.utils

fun <V> Iterable<V>.sumByFloat(func: (V) -> Float): Float {
    var sum = 0f
    for(entry in this){
        sum += func(entry)
        if(sum.isInfinite() || sum.isNaN()) return sum
    }
    return sum
}

fun <V> List<V>.sumByFloat(func: (V) -> Float): Float {
    var sum = 0f
    for(entry in this){
        sum += func(entry)
        if(sum.isInfinite() || sum.isNaN()) return sum
    }
    return sum
}

fun <V> MutableList<V>.pop(): V? {
    if(isEmpty()) return null
    val last = last()
    removeAt(lastIndex)
    return last
}