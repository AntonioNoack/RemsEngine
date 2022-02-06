package me.anno.remsstudio.objects.modes

import me.anno.language.translation.NameDesc
import java.util.*

enum class ArraySelectionMode(val naming: NameDesc){
    ROUND_ROBIN(NameDesc("Round-Robin")){
        override operator fun get(index: Int, length: Int, random: Random): Int {
            return index % length
        }
    },
    RANDOM(NameDesc("Random")){
        override operator fun get(index: Int, length: Int, random: Random): Int {
            return random.nextInt(length)
        }
    };
    // weighted mode?
    abstract operator fun get(index: Int, length: Int, random: Random): Int
}