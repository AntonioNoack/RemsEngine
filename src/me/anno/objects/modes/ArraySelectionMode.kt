package me.anno.objects.modes

import java.util.*

enum class ArraySelectionMode(val displayName: String){
    ROUND_ROBIN("Round-Robin"){
        override operator fun get(index: Int, length: Int, random: Random): Int {
            return index % length
        }
    },
    RANDOM("Random"){
        override operator fun get(index: Int, length: Int, random: Random): Int {
            return random.nextInt(length)
        }
    };
    // weighted mode?
    abstract operator fun get(index: Int, length: Int, random: Random): Int
}