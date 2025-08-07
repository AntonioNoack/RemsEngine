package me.anno.tests.utils

import me.anno.Engine
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("LoggingFileTest")

fun main() {
    for (i in 0 until 4096) {
        LOGGER.info("Message $i")
    }
    Engine.requestShutdown()
}