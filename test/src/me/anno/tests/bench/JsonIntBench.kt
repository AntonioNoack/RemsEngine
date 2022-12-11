package me.anno.tests.bench

import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.utils.Clock

fun main() {

    val length = 1 shl 20
    val size = 3000
    val ints = IntArray(length) { it % size }

    // (first run; still matters in later runs anyways; up to 50% faster then, from 30ns/e -> 20ns/e for 1<<20)
    // using a buffer large enough saves 46ns -> 34ns, so 26% of time (1<<20 elements)
    // or 31ns -> 22ns (1<<24 elements)
    // just being one byte too large causes an increase of 4ns/element
    for (tries in 0 until 10) {

        val writer = TextWriter(4854397, InvalidRef)

        val clock = Clock()
        clock.start()

        writer.writeIntArray("x", ints)
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

        lateinit var asInts: IntArray
        TextReader(asText, InvalidRef).readProperty(object : Saveable() {
            override fun readIntArray(name: String, values: IntArray) {
                asInts = values
            }

            override fun isDefaultValue(): Boolean = false
            override val approxSize get() = 0
            override val className get() = ""
        })

        clock.stop("toIntArray", length)

        for (i in 0 until length) {
            if (ints[i] != asInts[i]) {
                throw Exception()
            }
        }

    }

}