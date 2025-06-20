package me.anno.bench

import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.Clock
import me.anno.utils.assertions.assertEquals

fun main() {

    val length = 1 shl 20
    val size = 3000
    val values = IntArray(length) { it % size }

    // (first run; still matters in later runs anyway; up to 50% faster then, from 30ns/e -> 20ns/e for 1<<20)
    // using a buffer large enough saves 46ns -> 34ns, so 26% of time (1<<20 elements)
    // or 31ns -> 22ns (1<<24 elements)
    // just being one byte too large causes an increase of 4ns/element
    for (tries in 0 until 10) {

        val writer = JsonStringWriter(4854397, InvalidRef)

        val clock = Clock("JsonIntBench")
        clock.start()

        writer.writeIntArray("x", values)
        val asText = writer.toString()

        clock.stop("toText", length)

        if (tries == 0) {
            println(asText.length)
            if (asText.length > 100) {
                println(asText.substring(0, 100))
                println(asText.substring(asText.length - 100, asText.length))
            } else {
                println(asText)
            }
        }

        clock.start()

        var readValues: IntArray? = null
        JsonStringReader(asText, InvalidRef).readProperty(object : Saveable() {
            override fun setProperty(name: String, value: Any?) {
                readValues = value as? IntArray ?: return super.setProperty(name, value)
            }

            override fun isDefaultValue(): Boolean = false
            override val approxSize get() = 0
            override val className get() = ""
        })

        clock.stop("toIntArray", length)

        val readValues1 = readValues!!
        for (i in 0 until length) {
            assertEquals(values[i], readValues1[i])
        }
    }
}