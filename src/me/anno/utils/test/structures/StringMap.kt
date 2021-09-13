package me.anno.utils.test.structures

import me.anno.io.text.TextReader
import me.anno.io.utils.StringMap
import org.apache.logging.log4j.LogManager

fun main() {

    val logger = LogManager.getLogger("StringMap")

    val map = StringMap()
    map["debug.ui.enableVsync"] = true
    logger.info(map["debug.ui.enableVsync", true]) // true, correct
    map["debug.ui.enableVsync"] = false
    logger.info(map["debug.ui.enableVsync", true]) // false, correct

    val asMap = TextReader.clone(map) as StringMap
    logger.info(asMap["debug.ui.enableVsync"]) // null, incorrect

}