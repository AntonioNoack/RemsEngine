package me.anno.tests.structures

import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotContains
import me.anno.utils.assertions.assertNotNull
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test
import kotlin.test.assertSame

class SerializablePropertiesTest {

    class TestClass : Saveable() {

        var publicName = "public name"
        private var privateName = "private name"

        @NotSerializedProperty
        var notSerializable = "not serializable"

        @SerializedProperty
        private var serializable = "serializable"

        @SerializedProperty("anotherName")
        var withDifferentName = "different name"

        @SerializedProperty("", true)
        var savingZero = 0

        var notSavingZero = 0

        override val approxSize get() = 1

        override fun isDefaultValue(): Boolean = false

        override fun save(writer: BaseWriter) {
            super.save(writer)
            saveSerializableProperties(writer)
        }

        override fun setProperty(name: String, value: Any?) {
            if (!setSerializableProperty(name, value)) {
                super.setProperty(name, value)
            }
        }
    }

    @Test
    fun testSerialization() {

        val logger = LogManager.getLogger("SerializationTest")

        registerCustomClass(TestClass())
        val instance = TestClass()
        val text = instance.toString()
        logger.info(text)

        assertContains("S:publicName", text)
        assertContains("S:anotherName", text)
        assertNotContains("S:withDifferentName", text)
        assertNotContains("S:privateName", text)
        assertContains("i:savingZero", text)
        assertNotContains("i:notSavingZero", text)

        val copiedInstance = JsonStringReader.read(text, InvalidRef, false)
        logger.info(copiedInstance)
    }

    class TestClassA : Saveable() {
        var b: TestClassB? = null
        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeObject(this, "b", b)
        }

        override fun setProperty(name: String, value: Any?) {
            when (name) {
                "b" -> b = value as? TestClassB
                else -> super.setProperty(name, value)
            }
        }
    }

    class TestClassB : Saveable() {
        var a: TestClassA? = null
        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeObject(this, "a", a)
        }

        override fun setProperty(name: String, value: Any?) {
            when (name) {
                "a" -> a = value as? TestClassA
                else -> super.setProperty(name, value)
            }
        }
    }

    @Test
    fun testPointers() {
        registerCustomClass(TestClassA::class)
        registerCustomClass(TestClassB::class)
        val a = TestClassA()
        val b = TestClassB()
        a.b = b
        b.a = a
        val asString = JsonStringWriter.toText(a, InvalidRef)
        assertEquals(2, asString.split("\"i:*ptr\":1").size, asString)
        val parsed = JsonStringReader.readFirst(asString, InvalidRef, TestClassA::class)
        assertNotNull(parsed)
        assertNotNull(parsed.b)
        assertNotNull(parsed.b?.a)
        assertSame(parsed, parsed.b?.a)
    }
}
