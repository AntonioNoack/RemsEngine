package me.anno.utils.structures.arrays

object DoubleArrays {

    fun DoubleArray.accumulate() {
        var sum = 0.0
        for (index in indices) {
            val value = this[index]
            sum += value
            this[index] = sum
        }
    }

}