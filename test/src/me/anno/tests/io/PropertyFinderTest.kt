package me.anno.tests.io

import me.anno.io.base.BaseWriter
import me.anno.io.find.PropertyFinder
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertSame
import org.junit.jupiter.api.Test

class PropertyFinderTest {

    class OuterClass : Saveable() {
        var fieldA = InnerClass()
        var fieldB = InnerClass()
        var fieldC = MiddleClass()

        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeObject(null, "fieldA", fieldA)
            writer.writeObject(null, "fieldB", fieldB)
            writer.writeObject(null, "fieldC", fieldC)
        }
    }

    class MiddleClass : Saveable() {
        var fieldC = InnerClass()

        override fun save(writer: BaseWriter) {
            super.save(writer)
            writer.writeObject(null, "fieldC", fieldC)
        }
    }

    class InnerClass : Saveable()

    @Test
    fun testGetName() {
        val instance = OuterClass()
        assertEquals("fieldA", PropertyFinder.getName(instance, instance.fieldA))
        assertEquals("fieldB", PropertyFinder.getName(instance, instance.fieldB))
        assertEquals("fieldC", PropertyFinder.getName(instance, instance.fieldC))
        assertEquals("fieldC/fieldC", PropertyFinder.getName(instance, instance.fieldC.fieldC))
    }

    @Test
    fun testGetValue() {
        val instance = OuterClass()
        assertSame(instance.fieldA, PropertyFinder.getValue(instance, "fieldA"))
        assertSame(instance.fieldB, PropertyFinder.getValue(instance, "fieldB"))
        assertSame(instance.fieldC, PropertyFinder.getValue(instance, "fieldC"))
        assertSame(instance.fieldC.fieldC, PropertyFinder.getValue(instance, "fieldC/fieldC"))
    }
}