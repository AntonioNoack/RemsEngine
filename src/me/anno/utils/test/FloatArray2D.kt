package me.anno.utils.test

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.Saveable
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter

fun main() {

    val writer = TextWriter(false)
    writer.writeFloatArray2D("x", Array(5) { FloatArray(5) { if (it < 3) it.toFloat() else 0f } })
    println(writer.data.toString())

    val reader = TextReader(writer.data.toString())
    reader.readProperty(object : Saveable() {

        override fun readFloatArray2D(name: String, values: Array<FloatArray>) {
            println(
                "$name: ${
                    values.joinToString(",", "[", "]") { fa ->
                        fa.joinToString(",", "[", "]") { it.toInt().toString() }
                    }
                }"
            )
        }

        override fun isDefaultValue(): Boolean = false
        override val approxSize: Int = -1
        override val className: String = ""
    })

    val mesh = Mesh()
    mesh.positions = FloatArray(18) { it.toFloat() % 5f }
    mesh.indices = IntArray(10) { it }
    println(TextWriter.toText(mesh, false))


}