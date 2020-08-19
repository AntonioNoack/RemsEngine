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