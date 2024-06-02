package me.anno.tests.io.files

import me.anno.io.files.Reference.getReference
import org.apache.logging.log4j.LogManager
import kotlin.test.assertNotNull
import kotlin.test.assertNull

fun main() {
    val logger = LogManager.getLogger("FileTest")
    getReference("res://meshes/arrowX.obj").readText { txt, err ->
        logger.info(txt)
        assertNull(err)
        assertNotNull(txt)
    }
}
