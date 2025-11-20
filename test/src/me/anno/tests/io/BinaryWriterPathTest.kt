package me.anno.tests.io

import me.anno.io.base.BaseWriter
import me.anno.io.binary.BinaryReader
import me.anno.io.binary.BinaryWriter
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * there was a bug, where a Path wasn't correctly serialized in binary format;
 * this simplified the test until I found and fixed the issue :)
 * */
class BinaryWriterPathTest {

    class X : Saveable() {

        var type = 'x'.code

        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeInt("z", type)
        }

        override fun setProperty(name: String, value: Any?) {
            if (name == "z" && value is Int) type = value
            else super.setProperty(name, value)
        }

        override fun equals(other: Any?): Boolean {
            return other is X && other.type == type
        }

        override fun hashCode(): Int {
            return type.hashCode()
        }
    }

    class Y(var xs: List<X> = emptyList()) : Saveable() {
        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeNullableObjectList(this, "k", xs)
        }

        override fun setProperty(name: String, value: Any?) {
            @Suppress("UNCHECKED_CAST")
            if (name == "k") xs = value as List<X>
            else super.setProperty(name, value)
        }
    }

    val xs = listOf(X(), X())

    // test all properties of a class...
    @Test
    fun testBrokenSerialization() {
        registerCustomClass(X())
        registerCustomClass(Y())
        val bos = ByteArrayOutputStream()
        val writer = BinaryWriter(bos, InvalidRef)
        val instance = Y(xs)
        writer.add(instance)
        writer.writeAllInList()
        writer.close()
        bos.close()
        val bytes = bos.toByteArray()
        if (false) println(
            bytes
                .joinToString {
                    if (it >= 32 && it <= 'z'.code) "'${it.toInt().toChar()}'"
                    else it.toInt().toString()
                }.replace("', '", "")
        )
        val bis = ByteArrayInputStream(bytes)
        val reader = BinaryReader(bis, InvalidRef)
        reader.readAllInList()
        val clone = reader.allInstances.firstInstance2(Y::class)
        assertEquals(xs, clone.xs)
    }
}