package me.anno.tests.structures

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.io.json.saveable.JsonStringReader
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

    override val className get() = "Test"

    override val approxSize get() = 1

    override fun isDefaultValue(): Boolean = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun readSomething(name: String, value: Any?) {
        if (!readSerializableProperty(name, value)) {
            super.readSomething(name, value)
        }
    }

}

fun main() {

    val logger = LogManager.getLogger("SerializationTest")

    val instance = TestClass()
    val text = instance.toString()
    logger.info(text)

    ISaveable.registerCustomClass("Test") { TestClass() }
    val copiedInstance = JsonStringReader.read(text, InvalidRef, false)
    logger.info(copiedInstance)

}