package me.anno.tests.io.files

import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager
import kotlin.test.assertNotNull
import kotlin.test.assertNull

fun main() {
    val logger = LogManager.getLogger("FileTest")
    res.getChild("meshes/arrowX.obj").readText { txt, err ->
        logger.info(txt)
        assertNull(err)
        assertNotNull(txt)
    }
}
