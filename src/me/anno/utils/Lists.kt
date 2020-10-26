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

fun List<Double>.accumulate(): List<Double> {
    val accumulator = ArrayList<Double>()
    var sum = 0.0
    for(value in this){
        sum += value
        accumulator += sum
    }
    return accumulator
}

val <V> Sequence<V>.size get() = count { true }

fun <V> List<V>.getOrPrevious(index: Int) = if(index > 0) this[index-1] else this.getOrNull(0)

fun <V> List<V>.one(func: (V) -> Boolean): Boolean {
    for(entry in this){
        if(func(entry)) return true
    }
    return false
}

fun <V> Set<V>.one(func: (V) -> Boolean): Boolean {
    for(entry in this){
        if(func(entry)) return true
    }
    return false
}

fun <V> Sequence<V>.one(func: (V) -> Boolean): Boolean {
    for(entry in this){
        if(func(entry)) return true
    }
    return false
}

fun <K, V> Map<K, V>.one(func: (Map.Entry<K, V>) -> Boolean): Boolean {
    for(entry in this){
        if(func(entry)) return true
    }
    return false
}

fun <V> List<V>.indexOf2(v: V, i0: Int, minus1: Boolean): Int {
    for(i in i0 until size){
        if(this[i] == v) return i
    }
    return if(minus1) -1 else size
}