package me.anno.tests.files

import me.anno.io.files.Reference.getReference
import org.apache.logging.log4j.LogManager

fun main() {
    val logger = LogManager.getLogger("FileTest")
    logger.info(getReference("res://meshes/arrowX.obj").readTextSync())
}
