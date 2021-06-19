package me.anno.utils.test

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.serialization.NotSerializableProperty
import me.anno.io.serialization.SerializableProperty

class Serialization : Saveable() {

    var publicName = "public name"
    private var privateName = "private name"

    @NotSerializableProperty
    var notSerializable = "not serializable"

    @SerializableProperty
    private var serializable = "serializable"

    @SerializableProperty("anotherName")
    var withDifferentName = "different name"

    @SerializableProperty("", true)
    var savingZero = 0

    override fun getClassName(): String = "Test"

    override fun getApproxSize(): Int = 1

    override fun isDefaultValue(): Boolean = false

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

}

fun main() {

    val instance = Serialization()
    println(instance.toString())

}