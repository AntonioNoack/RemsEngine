package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.input.AnyArrayPanel2
import me.anno.engine.ui.input.ComponentUI
import me.anno.engine.ui.input.ComponentUI.writeTo
import me.anno.io.ISaveable.Companion.getReflections
import me.anno.studio.Inspectable
import me.anno.studio.InspectableProperty
import me.anno.ui.debug.TestStudio.Companion.testUI3

// todo some content seems to be missing / invisible: with base >= 15

fun main() {
    testUI3("Edit Huge Arrays") {
        class Data : Inspectable {
            var data = IntArray(43) { it }
        }

        val instance = Data()
        val reflections = getReflections(instance)
        val property = reflections.allProperties["data"]!!
        val cleanInstance = Data()
        val value = instance.data
        val iProperty = InspectableProperty(listOf(instance), property, cleanInstance)
        val arrayType = ComponentUI.getArrayType(iProperty, value.iterator())!!
        object : AnyArrayPanel2("Data", "", arrayType, style) {
            override fun onChange() {
                property[instance] = values.writeTo(value)
            }
        }.apply {
            base = 4
            setValues(value.toList())
        }
    }
}