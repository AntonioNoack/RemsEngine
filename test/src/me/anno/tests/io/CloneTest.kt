package me.anno.tests.io

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.OfficialExtensions
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotSame
import me.anno.utils.assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class CloneTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testClone() {
        OfficialExtensions.initForTests()
        var failedClasses = 0
        var passedClasses = 0
        for ((_, entry) in Saveable.objectTypeRegistry) {
            val sample = entry.sampleInstance
            if (sample is PrefabSaveable) {
                try {
                    testClone(sample)
                    passedClasses++
                } catch (e: Exception) {
                    e.printStackTrace()
                    failedClasses++
                }
            }
        }
        assertEquals(0, failedClasses)
    }

    fun testClone(original: PrefabSaveable) {
        val clone = original.clone()
        assertSame(original::class, clone::class)
        assertNotSame(original, clone)
        val originalAsText = JsonStringWriter.toText(original, InvalidRef)
        val cloneAsText = JsonStringWriter.toText(clone, InvalidRef)
        assertEquals(originalAsText, cloneAsText)
    }
}