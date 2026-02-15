package me.anno.tools

import me.anno.utils.OS
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("NativeArrayListBuilder")

/**
 * copy LongArrayList into all other native types
 * */
fun main() {
    val type0 = "Float"
    val types = listOf(
        "Int" to "0",
        "Long" to "0L",
        "Float" to "0f",
        "Double" to "0.0",
        "Short" to "0.toShort()",
        "Byte" to "0.toByte()"
    )
    assertNotEquals("Int", type0)
    val zero0 = types.first { (type, _) -> type == type0 }.second
    val project = OS.engineProject
    val folder = project.getChild("src/me/anno/utils/structures/arrays")
    val srcFile = folder.getChild("${type0}ArrayList.kt")
    assertTrue(srcFile.exists, "Missing $srcFile")
    srcFile.readText { txt, err ->
        if (txt != null) {
            for ((type, zero) in types) {
                if (type == type0) continue
                val dstFile = folder.getChild("${type}ArrayList.kt")
                if (dstFile.exists) {
                    val dstText = txt
                        .replace(type0, type)
                        .replace(zero0, zero)
                    dstFile.writeText(dstText)
                } else LOGGER.warn("Missing $dstFile")
            }
        } else err?.printStackTrace()
    }
}

