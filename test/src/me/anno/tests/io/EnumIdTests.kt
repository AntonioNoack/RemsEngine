package me.anno.tests.io

import me.anno.ecs.Component
import me.anno.ecs.annotations.ExtendableEnum
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.language.translation.NameDesc
import me.anno.utils.Reflections.getEnumId
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotSame
import org.junit.jupiter.api.Test

class EnumIdTests {

    enum class EnumWithoutId {
        VALUE_0,
        VALUE_1,
        VALUE_2
    }

    enum class EnumWithId(val id: Int) {
        VALUE_0(10),
        VALUE_1(11),
        VALUE_2(12)
    }

    class ExtendableEnumImpl(override val id: Int) : ExtendableEnum {

        override val nameDesc: NameDesc
            get() = NameDesc("$id")

        override val values: List<ExtendableEnum>
            get() = entries

        init {
            entries.add(this)
        }

        companion object {
            val entries = ArrayList<ExtendableEnumImpl>()

            val VALUE_0 = ExtendableEnumImpl(20)
            val VALUE_1 = ExtendableEnumImpl(21)
            val VALUE_2 = ExtendableEnumImpl(22)
        }
    }

    class WrapperWithoutId : Component() {
        var type = EnumWithoutId.VALUE_0
    }

    class WrapperWithId : Component() {
        var type = EnumWithId.VALUE_0
    }

    class WrapperWithExtendable : Component() {
        var type = ExtendableEnumImpl.VALUE_0
    }

    @Test
    fun testCanGetID() {
        assertEquals(0, getEnumId(EnumWithoutId.VALUE_0))
        assertEquals(1, getEnumId(EnumWithoutId.VALUE_1))
        assertEquals(2, getEnumId(EnumWithoutId.VALUE_2))
        assertEquals(10, getEnumId(EnumWithId.VALUE_0))
        assertEquals(11, getEnumId(EnumWithId.VALUE_1))
        assertEquals(12, getEnumId(EnumWithId.VALUE_2))
        assertEquals(20, getEnumId(ExtendableEnumImpl.VALUE_0))
        assertEquals(21, getEnumId(ExtendableEnumImpl.VALUE_1))
        assertEquals(22, getEnumId(ExtendableEnumImpl.VALUE_2))
    }

    @Test
    fun testSavesCorrectID() {
        registerCustomClass(WrapperWithoutId())
        registerCustomClass(WrapperWithId())
        registerCustomClass(WrapperWithExtendable())

        testSaveId(WrapperWithoutId())
        testSaveId(WrapperWithoutId().apply { type = EnumWithoutId.VALUE_1 })
        testSaveId(WrapperWithoutId().apply { type = EnumWithoutId.VALUE_2 })
        testSaveId(WrapperWithId())
        testSaveId(WrapperWithId().apply { type = EnumWithId.VALUE_1 })
        testSaveId(WrapperWithId().apply { type = EnumWithId.VALUE_2 })
        testSaveId(WrapperWithoutId())
        testSaveId(WrapperWithExtendable().apply { type = ExtendableEnumImpl.VALUE_1 })
        testSaveId(WrapperWithExtendable().apply { type = ExtendableEnumImpl.VALUE_2 })
    }

    fun testSaveId(original: Component) {
        val getter = original.javaClass.getMethod("getType")
        val asText = JsonStringWriter.toText(original, InvalidRef)
        assertContains(getEnumId(getter.invoke(original)).toString(), asText)
        val clone = JsonStringReader.readFirst(asText, InvalidRef, original::class)
        assertNotSame(original, clone)
        assertEquals(getter.invoke(original), getter.invoke(clone))
    }
}