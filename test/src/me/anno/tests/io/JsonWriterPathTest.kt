package me.anno.tests.io

import me.anno.ecs.prefab.change.Path
import me.anno.engine.projects.FileEncoding
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.saveable.UnknownSaveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.junit.jupiter.api.Test

/**
 * JSON-reader didn't support homogenous/heterogeneous 2d-arrays
 * */
class JsonWriterPathTest {

    class Circular(var id: Int = 0, var linked: Circular? = null) : Saveable() {

        init {
            // create a circle
            linked?.linked = this
        }

        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeObject(null, "linked", linked)
            writer.writeInt("id", id)
        }

        override fun setProperty(name: String, value: Any?) {
            if (name == "linked" && value is Circular?) linked = value
            else if (name == "id" && value is Int) id = value
            else super.setProperty(name, value)
        }

        override fun equals(other: Any?): Boolean {
            return other is Circular && id == other.id &&
                    linked?.id == other.linked?.id
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

    val instances0 = listOf(
        // complex objects with references like paths
        Path.ROOT_PATH,
        // objects with circular references
           Circular(1),
           Circular(2, Circular(3)),
    )

    val instances = instances0.map { listOf(listOf(it)) }

    fun testAnyPropertyWriter(encoding: FileEncoding) {

        // register classes
        registerCustomClass(UnknownSaveable())
        registerCustomClass(Path())
        registerCustomClass(Circular())

        // prepare mega-instance will all properties
        val instance = UnknownSaveable()
        for ((idx, v) in instances.withIndex()) {
            instance.setProperty("p$idx", v)
        }

        // serialization
        val bytes = encoding.encode(instance, InvalidRef)
        println(bytes.decodeToString())

        // deserialization
        val clone = encoding.decode(bytes, InvalidRef, false)
            .firstInstance2(UnknownSaveable::class)

        // equality check
        for ((idx, v) in instances.withIndex()) {
            checkEquals(v, clone["p$idx"])
        }
    }

    @Test
    fun testPrettyJsonSerialization() {
        testAnyPropertyWriter(FileEncoding.PRETTY_JSON)
    }

    private fun checkEquals(v: Any?, vi: Any?) {
        when (v) {
            is ByteArray -> assertTrue(v.contentEquals(vi as? ByteArray))
            is ShortArray -> assertTrue(v.contentEquals(vi as? ShortArray))
            is CharArray -> assertTrue(v.contentEquals(vi as? CharArray))
            is IntArray -> assertTrue(v.contentEquals(vi as? IntArray))
            is LongArray -> assertTrue(v.contentEquals(vi as? LongArray))
            is FloatArray -> assertTrue(v.contentEquals(vi as? FloatArray))
            is DoubleArray -> assertTrue(v.contentEquals(vi as? DoubleArray))
            is List<*> -> {
                vi as List<*>
                assertEquals(v.size, vi.size)
                for (i in v.indices) {
                    checkEquals(v[i], vi[i])
                }
            }
            else -> assertEquals(v, vi)
        }
    }
}