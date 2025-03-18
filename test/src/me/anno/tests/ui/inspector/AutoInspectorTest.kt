package me.anno.tests.ui.inspector

import me.anno.config.DefaultConfig.style
import me.anno.ecs.annotations.Type
import me.anno.engine.inspector.Inspectable
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.ui.base.groups.PanelGroup
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.input.BooleanInput
import me.anno.ui.input.ColorInput
import me.anno.ui.input.FileInput
import me.anno.ui.input.FloatInput
import me.anno.ui.input.IntInput
import me.anno.ui.input.TextInput
import me.anno.utils.Color.toVecRGB
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.OS.documents
import me.anno.utils.OS.pictures
import me.anno.utils.algorithms.Recursion
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.Collections.filterIsInstance2
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

class AutoInspectorTest {

    abstract class Wrapper : Saveable(), Inspectable

    @Test
    fun testByteInput() {
        class ByteWrapper : Wrapper() {
            var value: Byte = 5
        }

        val instance = ByteWrapper()
        instance.value = 7
        testIntInput(instance)
    }

    @Test
    fun testShortInput() {
        class ShortWrapper : Wrapper() {
            var value: Short = 5
        }

        val instance = ShortWrapper()
        instance.value = 7
        testIntInput(instance)
    }

    @Test
    fun testIntInput() {
        class IntWrapper : Wrapper() {
            var value: Int = 5
        }

        val instance = IntWrapper()
        instance.value = 7
        testIntInput(instance)
    }

    @Test
    fun testLongInput() {
        class LongWrapper : Wrapper() {
            var value: Long = 5
        }

        val instance = LongWrapper()
        instance.value = 7
        testIntInput(instance)
    }

    fun testIntInput(instance: Wrapper) {
        registerCustomClass(instance::class)
        val ui = PanelListY(style)
        instance.createInspector(ui, style)
        val tested = findPanel(ui, IntInput::class)
        assertTrue(tested.isInputAllowed)
        assertEquals(7L, tested.value)
        tested.onEmpty(0f, 0f)
        assertEquals(5L, tested.value)
    }

    @Test
    fun testFloatInput() {
        class FloatWrapper : Wrapper() {
            var value = 5f
        }

        val instance = FloatWrapper()
        instance.value = 7f
        testFloatInput(instance)
    }

    @Test
    fun testDoubleInput() {
        class DoubleWrapper : Wrapper() {
            var value = 5.0
        }

        val instance = DoubleWrapper()
        instance.value = 7.0
        testFloatInput(instance)
    }

    fun testFloatInput(instance: Wrapper) {
        registerCustomClass(instance::class)
        val ui = PanelListY(style)
        instance.createInspector(ui, style)
        val tested = findPanel(ui, FloatInput::class)
        assertTrue(tested.isInputAllowed)
        assertEquals(7.0, tested.value)
        tested.onEmpty(0f, 0f)
        assertEquals(5.0, tested.value)
    }

    @Test
    fun testBooleanInput() {
        class BooleanWrapper : Wrapper() {
            var value = true
        }

        val instance = BooleanWrapper()
        instance.value = false
        registerCustomClass(instance::class)
        val ui = PanelListY(style)
        instance.createInspector(ui, style)
        val tested = findPanel(ui, BooleanInput::class)
        assertTrue(tested.isInputAllowed)
        assertEquals(false, tested.value)
        tested.onEmpty(0f, 0f)
        assertEquals(true, tested.value)
    }

    @Test
    fun testStringInput() {
        class StringWrapper : Wrapper() {
            var value = "default"
        }

        val instance = StringWrapper()
        instance.value = "custom"
        registerCustomClass(instance::class)
        val ui = PanelListY(style)
        instance.createInspector(ui, style)
        val tested = findPanel(ui, TextInput::class)
        assertTrue(tested.isInputAllowed)
        assertEquals("custom", tested.value)
        tested.onEmpty(0f, 0f)
        assertEquals("default", tested.value)
    }

    @Test
    fun testFileReferenceInput() {
        class FileRefWrapper : Wrapper() {
            var value = documents
        }

        val instance = FileRefWrapper()
        instance.value = pictures
        registerCustomClass(instance::class)
        val ui = PanelListY(style)
        instance.createInspector(ui, style)
        val tested = findPanel(ui, FileInput::class)
        assertTrue(tested.isInputAllowed)
        assertEquals(pictures, tested.value)
        tested.onEmpty(0f, 0f)
        assertEquals(documents, tested.value)
    }

    @Test
    fun testColor3Input() {
        class Color3Wrapper : Wrapper() {
            @Type("Color3")
            var value = 0x123456.toVecRGB()
        }

        val instance = Color3Wrapper()
        instance.value = 0x789abc.toVecRGB()
        registerCustomClass(instance::class)
        val ui = PanelListY(style)
        instance.createInspector(ui, style)
        val tested = findPanel(ui, ColorInput::class)
        assertTrue(tested.isInputAllowed)
        assertEquals(0x789abc.toVecRGBA(), tested.value, 1e-3)
        tested.onEmpty(0f, 0f)
        assertEquals(0x123456.toVecRGBA(), tested.value, 1e-3)
    }

    @Test
    fun testColor4Input() {
        class Color4Wrapper : Wrapper() {
            @Type("Color4")
            var value = 0x77123456.toVecRGBA()
        }

        val instance = Color4Wrapper()
        instance.value = 0x55789abc.toVecRGBA()
        registerCustomClass(instance::class)
        val ui = PanelListY(style)
        instance.createInspector(ui, style)
        val tested = findPanel(ui, ColorInput::class)
        assertTrue(tested.isInputAllowed)
        assertEquals(0x55789abc.toVecRGBA(), tested.value, 1e-3)
        tested.onEmpty(0f, 0f)
        assertEquals(0x77123456.toVecRGBA(), tested.value, 1e-3)
    }

    private fun <V : Any> findPanel(ui: PanelGroup, clazz: KClass<V>): V {
        return Recursion.findRecursive(ui) { item, remaining ->
            remaining.addAll(item.children.filterIsInstance2(PanelGroup::class))
            clazz.safeCast(item)
        }!!
    }
}