package me.anno.tests.structures

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import org.apache.logging.log4j.LogManager

fun main() {

    val logger = LogManager.getLogger("FloatArray2D")

    val writer = TextWriter(InvalidRef)
    writer.writeFloatArray2D("x", Array(5) { FloatArray(5) { if (it < 3) it.toFloat() else 0f } })
    logger.info(writer.toString())

    val reader = TextReader(writer.toString(), InvalidRef)
    reader.readProperty(object : Saveable() {

        override fun readFloatArray2D(name: String, values: Array<FloatArray>) {
            logger.info(
                "$name: ${
                    values.joinToString(",", "[", "]") { fa ->
                        fa.joinToString(",", "[", "]") { it.toInt().toString() }
                    }
                }"
            )
        }

        override fun isDefaultValue(): Boolean = false
        override val approxSize: Int = -1
        override val className = ""
    })

    val mesh = Mesh()
    mesh.positions = FloatArray(18) { it.toFloat() % 5f }
    mesh.indices = IntArray(10) { it }
    logger.info(TextWriter.toText(mesh, InvalidRef))


}