package me.anno.tests.io.files

import me.anno.utils.OS.res
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertNull
import org.apache.logging.log4j.LogManager

fun main() {
    val logger = LogManager.getLogger("FileTest")
    res.getChild("meshes/arrowX.obj").readText { txt, err ->
        logger.info(txt)
        assertNull(err)
        assertNotNull(txt)
    }
}
