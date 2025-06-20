package me.anno.bench

import me.anno.io.base.BaseWriter
import me.anno.io.binary.BinaryReader
import me.anno.io.binary.BinaryWriter
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.tests.LOGGER
import me.anno.utils.Clock
import java.io.ByteArrayOutputStream

fun main() {

    // at the first much slower, then it gets optimized somehow...
    // down to the same 12ns/e like text...

    class TestClass : Saveable() {

        var x: IntArray? = null

        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeIntArray("x", x!!)
        }

        override fun setProperty(name: String, value: Any?) {
            when (name) {
                "x" -> x = value as? IntArray
                else -> super.setProperty(name, value)
            }
        }
    }
    registerCustomClass(TestClass())

    val length = 1 shl 20
    val size = 3000
    val ints = IntArray(length) { it % size }

    for (tries in 0 until 100) {

        val bos = ByteArrayOutputStream(length * 4 + 1000)
        val writer = BinaryWriter(bos, InvalidRef)

        val clock = Clock(LOGGER)
        clock.start()

        writer.add(TestClass().apply { x = ints })
        writer.writeAllInList()
        val asText = bos.toByteArray()

        clock.stop("toText", length)

        if (tries == 0) {
            println(asText.size)
        }

        val reader = BinaryReader(asText.inputStream())
        clock.start()

        val asInts = reader.run {
            readAllInList()
            finish()
            allInstances.firstOrNull() as TestClass
        }.x!!

        clock.stop("toIntArray", length)

        for (i in 0 until length) {
            if (ints[i] != asInts[i]) {
                throw Exception()
            }
        }
    }
}