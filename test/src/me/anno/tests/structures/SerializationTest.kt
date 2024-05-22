package me.anno.tests.structures

import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.LogManager

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

fun main() {

    val logger = LogManager.getLogger("SerializationTest")

    registerCustomClass(TestClass())
    val instance = TestClass()
    val text = instance.toString()
    logger.info(text)

    assertTrue("S:publicName" in text)
    assertTrue("S:anotherName" in text)
    assertTrue("S:withDifferentName" !in text)

    val copiedInstance = JsonStringReader.read(text, InvalidRef, false)
    logger.info(copiedInstance)
}