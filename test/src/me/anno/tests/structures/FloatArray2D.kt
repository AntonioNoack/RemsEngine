package me.anno.tests.structures

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import org.apache.logging.log4j.LogManager

fun main() {

    val logger = LogManager.getLogger("FloatArray2D")

    val writer = JsonStringWriter(InvalidRef)
    writer.writeFloatArray2D("x", Array(5) { FloatArray(5) { if (it < 3) it.toFloat() else 0f } })
    logger.info(writer.toString())

    val reader = JsonStringReader(writer.toString(), InvalidRef)
    reader.readProperty(object : Saveable() {

        override fun setProperty(name: String, value: Any?) {
            val values = value as? Array<*> ?: return
            logger.info(
                "$name: ${
                    values.joinToString(",", "[", "]") { fa ->
                        fa as FloatArray
                        fa.joinToString(",", "[", "]") { it.toInt().toString() }
                    }
                }"
            )
        }

        override fun isDefaultValue(): Boolean = false
        override val approxSize get() = -1
        override val className get() = ""
    })

    val mesh = Mesh()
    mesh.positions = FloatArray(18) { it.toFloat() % 5f }
    mesh.indices = IntArray(10) { it }
    logger.info(JsonStringWriter.toText(mesh, InvalidRef))
}