package me.anno.graph.octtree

import me.anno.utils.LOGGER

fun main() {

    val max = 1 shl 2

    for (minIndex in 0 until max) {
        for (maxIndex in minIndex until max) {
            val or = minIndex or maxIndex
            val invOr = or.inv()
            val and = minIndex and maxIndex
            val invAnd = and.inv()
            for (index in 0 until max) {

                val invIndex = index.inv()

                val isBetween = or and invAnd // from 0 to 1
                val isZero = invOr and invIndex
                val isOne = and and index

                val answer = isBetween or isZero or isOne
                if (answer != -1) continue

                LOGGER.info("${index.toString(2)} is ok for ${minIndex.toString(2)} .. ${maxIndex.toString(2)}")
            }
        }
    }

}
