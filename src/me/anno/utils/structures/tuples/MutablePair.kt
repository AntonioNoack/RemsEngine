package me.anno.utils.structures.tuples

/**
 * mutable pair
 * */
class MutablePair<A, B> (var first: A, var second: B){

    operator fun component1() = first
    operator fun component2() = second

}