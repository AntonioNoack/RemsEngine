package me.anno.tests.io

import me.anno.ecs.components.mesh.material.Material
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertContains
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotEquals
import org.junit.jupiter.api.Test

class UnknownSaveableTest {

    @Test
    fun testSaving() {
        val uniqueClassName = ('a'..'z').shuffled().subList(0, 10).joinToString("")
        val data = Material()
        val correctSerialization = JsonStringWriter.toText(data, InvalidRef)
        val correctClass = "\"class\":\"Material\""
        val incorrectClass = "\"class\":\"$uniqueClassName\""
        assertContains(correctSerialization, correctClass)
        val incorrectSerialization = correctSerialization.replace(correctClass, incorrectClass)
        assertNotEquals(correctSerialization, incorrectSerialization)
        val unknownTypedClone = JsonStringReader.readFirst(incorrectSerialization, InvalidRef, Saveable::class)
        val testedSerialization = JsonStringWriter.toText(unknownTypedClone, InvalidRef)
        assertEquals(incorrectSerialization, testedSerialization)
    }
}