package me.anno.tests.engine

import me.anno.ecs.prefab.PropertySetter.setPropertyRespectingMask
import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PropertySetterTests {

    class SampleClass : Saveable() {

        var v2f = Vector2f()
        var v2d = Vector2d()

        var v3f = Vector3f()
        var v3d = Vector3d()

        var v4f = Vector4f()
        var v4d = Vector4d()

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
    fun testSetPropertyRespectingMasks() {
        testSetPropertyRespectingMasks("v2f", ::Vector2f)
        testSetPropertyRespectingMasks("v2d", ::Vector2d)
        testSetPropertyRespectingMasks("v3f", ::Vector3f)
        testSetPropertyRespectingMasks("v3d", ::Vector3d)
        testSetPropertyRespectingMasks("v4f", ::Vector4f)
        testSetPropertyRespectingMasks("v4d", ::Vector4d)
    }

    fun <V : Vector> testSetPropertyRespectingMasks(name: String, newVector: () -> V) {
        val instance = SampleClass()
        val properties = Saveable.getReflections(instance).allProperties
        val numComponents = newVector().numComponents
        val random = Random(1234)
        val expectedValue = newVector()
        for (mask in 0 until (1 shl numComponents)) {
            val newValue = newVector()
            for (i in 0 until numComponents) {
                newValue.setComp(i, random.nextDouble())
                if (mask.hasFlag(1 shl i)) {
                    expectedValue.setComp(i, newValue.getComp(i))
                }
            }
            val property = properties[name]!!
            setPropertyRespectingMask(newValue, mask, property, instance)
            assertEquals(expectedValue, property[instance])
        }
    }
}