package me.anno.tests.io

import me.anno.ecs.prefab.change.Path
import me.anno.engine.DefaultAssets.flatCube
import me.anno.engine.projects.FileEncoding
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.saveable.UnknownSaveable
import me.anno.utils.OS.documents
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i
import org.junit.jupiter.api.Test

/**
 * tests all data types (primitives, and a few Saveables) for all (de)serializers;
 * testing special unicode characters is done in a separate test
 * */
class CompleteFileEncodingTest {

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
        // all number types
        true, false,
        1.toByte(), (-64).toByte(),
        byteArrayOf(1, 2, 3),
        //  '9', '3',
        charArrayOf('0', '1'),
        2.toShort(), (-426).toShort(),
        shortArrayOf(6, 7, 8),
        3, -516265,
        intArrayOf(9, 10, 11),
        4L, -54416516313L,
        longArrayOf(12, 13, 14),
        -0f, 5f, -7.9f,
        floatArrayOf(15f, 16f, 17f),
        6.0, -98.0,
        doubleArrayOf(18.0, 19.0, 20.0),
        // strings
        "Hello World!",
        // files
        documents,
        // vectors
        Vector2i(1, 2),
        Vector3i(2, 4, 6),
        Vector4i(9, 5, 3, -5),
        Vector2f(1f, 2f),
        Vector3f(2f, 4f, 6f),
        Vector4f(9f, 5f, 3f, -5f),
        Vector2d(1.0, 2.0),
        Vector3d(2.0, 4.0, 6.0),
        Vector4d(9.0, 5.0, 3.0, -5.0),
        // planes
        Planef(0f, 1f, 2f, 3f),
        Planed(1.0, 2.0, 3.0, 4.0),
        // quaternions
        Quaternionf(1f, 2f, 3f, 4f),
        Quaterniond(-1.0, 2.0, -3.0, 4.0),
        // aabbs
        AABBf(-1f, -2f, -3f, 4f, 5f, 6f),
        AABBd(-1.0, -2.0, -3.0, 4.0, 5.0, 6.0),
        // matrices
        Matrix2f(0f, 1f, 2f, 3f),
        Matrix3x2f(0f, 1f, 2f, 3f, 4f, 5f),
        Matrix3f(0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f),
        Matrix4x3f(
            0f, 1f, 2f, 3f, 4f, 5f,
            6f, 7f, 8f, 9f, 10f, 11f
        ),
        Matrix4f(
            0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f,
            8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f
        ),
        Matrix2d(0.0, 1.0, 2.0, 3.0),
        Matrix3x2d(0.0, 1.0, 2.0, 3.0, 4.0, 5.0),
        Matrix3d(0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0),
        Matrix4x3d(
            0.0, 1.0, 2.0, 3.0, 4.0, 5.0,
            6.0, 7.0, 8.0, 9.0, 10.0, 11.0
        ),
        Matrix4d(
            0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0,
            8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0
        ),
        // complex objects with references like paths
        Path.fromString("a15,hi/b7,jo/c9,test"),
        // objects with circular references
        Circular(1),
        Circular(2, Circular(3)),
        flatCube,
        // todo heterogeneous object list
    )

    val instances1 = instances0.filter { !hasNativeArray(it) }.map { listOf(it, it) }
    val instances2 = instances1.filter { !isNativeArray(it[0]) }.map { listOf(it, it, it) } +
            instances0.filter { isNativeArray(it) }.map { listOf(it, it, it) }

    val instances = instances0// + instances1 + instances2

    fun hasNativeArray(v: Any): Boolean {
        return v is Boolean || v is Char || v is Number
    }

    fun isNativeArray(v: Any): Boolean {
        return when (v) {
            is ByteArray,
            is ShortArray,
            is CharArray,
            is IntArray,
            is LongArray,
            is FloatArray,
            is DoubleArray -> true
            else -> false
        }
    }

    // test all properties of a class...

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
        if (encoding != FileEncoding.BINARY) println(bytes.decodeToString())

        // deserialization
        val clone = encoding.decode(bytes, InvalidRef, false)
            .firstInstance2(UnknownSaveable::class)

        // equality check
        for ((idx, v) in instances.withIndex()) {
            checkEquals(v, clone["p$idx"])
        }
    }

    @Test
    fun testBinarySerialization() {
        testAnyPropertyWriter(FileEncoding.BINARY)
    }

    @Test
    fun testCompactJsonSerialization() {
        testAnyPropertyWriter(FileEncoding.COMPACT_JSON)
    }

    @Test
    fun testPrettyJsonSerialization() {
        testAnyPropertyWriter(FileEncoding.PRETTY_JSON)
    }

    @Test
    fun testYamlSerialization() {
        testAnyPropertyWriter(FileEncoding.YAML)
    }

    @Test
    fun testCompactXmlSerialization() {
        testAnyPropertyWriter(FileEncoding.COMPACT_XML)
    }

    @Test
    fun testPrettyXmlSerialization() {
        testAnyPropertyWriter(FileEncoding.PRETTY_XML)
    }

    @Test
    fun testNoUnnecessaryQuot() {
        val encoding = FileEncoding.PRETTY_XML
        val instance = UnknownSaveable()
        instance.setProperty("floats", floatArrayOf(1f))
        val xml = encoding.encode(instance, InvalidRef).decodeToString()
        assertTrue("&quot" !in xml, xml)
        println(xml)
    }

    companion object {
        fun checkEquals(v: Any?, vi: Any?, o: Any? = v) {
            when (v) {
                is Saveable -> assertEquals(v.toString(), vi.toString())
                is ByteArray -> {
                    val vj = vi as? ByteArray
                    assertTrue(v.contentEquals(vj), "${v.toList()} != ${vj?.toList()} of $o")
                }
                is ShortArray -> {
                    val vj = vi as? ShortArray
                    assertTrue(v.contentEquals(vj), "${v.toList()} != ${vj?.toList()} of $o")
                }
                is CharArray -> {
                    val vj = vi as? CharArray
                    assertTrue(v.contentEquals(vj), "${v.toList()} != ${vj?.toList()} of $o")
                }
                is IntArray -> {
                    val vj = vi as? IntArray
                    assertTrue(v.contentEquals(vj), "${v.toList()} != ${vj?.toList()} of $o")
                }
                is LongArray -> {
                    val vj = vi as? LongArray
                    assertTrue(v.contentEquals(vj), "${v.toList()} != ${vj?.toList()} of $o")
                }
                is FloatArray -> {
                    val vj = vi as? FloatArray
                    assertTrue(v.contentEquals(vj), "${v.toList()} != ${vj?.toList()} of $o")
                }
                is DoubleArray -> {
                    val vj = vi as? DoubleArray
                    assertTrue(v.contentEquals(vj), "${v.toList()} != ${vj?.toList()} of $o")
                }
                is List<*> -> {
                    vi as List<*>
                    assertEquals(v.size, vi.size)
                    for (i in v.indices) {
                        checkEquals(v[i], vi[i], v)
                    }
                }
                is Float -> {
                    assertEquals(v, vi as Float, 0f)
                }
                is Double -> {
                    assertEquals(v, vi as Double, 0.0)
                }
                else -> assertEquals(v, vi)
            }
        }
    }
}