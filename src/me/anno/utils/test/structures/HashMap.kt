package me.anno.utils.test.structures

import me.anno.utils.Clock
import me.anno.utils.structures.maps.HashMap2
import org.apache.logging.log4j.LogManager

fun main() {

    val logger = LogManager.getLogger("HashMap")

    val capacity0 = 16
    val capacity1 = 1 shl 16

    val clock = Clock()

    for (j in 0 until 2) {

        for ((index, map) in listOf(HashMap<Int, Int>(capacity0), HashMap2(capacity0)).withIndex()) {

            // small warmup
            for (i in (0 until capacity0).shuffled()) {
                map[i] = i
            }

            logger.info("Map $index:")

            clock.start()

            for (i in (0 until capacity1).shuffled()) {
                map[i] = i
            }

            clock.stop("added", -1.0)

            for (i in (0 until capacity1).shuffled()) {
                if (i !in map) {
                    map as HashMap2
                    val hash = i.hashCode()
                    val index = hash and map.capacityM1
                    logger.info(index)
                    logger.info(map.keyStore[index])
                    logger.info(map.valStore[index])
                    logger.info(map.overflow[index])
                    throw RuntimeException()
                }
            }

            clock.stop("checks", -1.0)

            for (i in (0 until capacity1 step 2).shuffled()) {
                map.remove(i)
            }

            clock.stop("removal", -1.0)

            for (i in (0 until capacity1 step 2).shuffled()) {
                if (i in map) {
                    throw RuntimeException()
                }
            }

            clock.stop("checks 2", -1.0)

            for (i in (1 until capacity1 step 2).shuffled()) {
                if (i !in map) {
                    throw RuntimeException()
                }
            }

            clock.stop("checks 3", -1.0)

            map.clear()

            clock.stop("clear", -1.0)

            logger.info()

        }

    }

}