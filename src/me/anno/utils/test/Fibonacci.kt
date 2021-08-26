package me.anno.utils.test

import me.anno.utils.LOGGER
import java.math.BigInteger

fun main() {

    val zahlen = ArrayList<BigInteger>()
    zahlen.add(BigInteger.ZERO)
    zahlen.add(BigInteger.ONE)

    for (i in 0 until 1000) {
        val index = zahlen.size
        val neueZahl = zahlen[index - 2] + zahlen[index - 1]
        zahlen.add(neueZahl)
        LOGGER.info(neueZahl)
    }

}