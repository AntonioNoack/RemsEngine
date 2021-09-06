package me.anno.utils.bench

import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.binary.BinaryReader
import me.anno.io.binary.BinaryWriter
import me.anno.utils.Clock
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

fun main() {

    // at the first much slower, then it gets optimized somehow...
    // down to the same 12ns/e like text...

    class TestClass : Saveable() {

        var x: IntArray? = null

        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeIntArray("x", x!!)
        }

        override fun readIntArray(name: String, values: IntArray) {
            x = values
        }
    }
    registerCustomClass(TestClass())

    val length = 1 shl 20
    val size = 3000
    val ints = IntArray(length) { it % size }

    for (tries in 0 until 100) {

        val bos = ByteArrayOutputStream(length * 4 + 1000)
        val writer = BinaryWriter(DataOutputStream(bos))

        val clock = Clock()
        clock.start()

        writer.add(TestClass().apply { x = ints })
        writer.writeAllInList()
        val asText = bos.toByteArray()

        clock.stop("toText", length)

        if (tries == 0) {
            println(asText.size)
        }

        val reader = BinaryReader(DataInputStream(asText.inputStream()))
        clock.start()

        val asInts = reader.run {
            readAllInList()
            content.values.first() as TestClass
        }.x!!

        clock.stop("toIntArray", length)

        for (i in 0 until length) {
            if (ints[i] != asInts[i]) {
                throw Exception()
            }
        }

    }

}