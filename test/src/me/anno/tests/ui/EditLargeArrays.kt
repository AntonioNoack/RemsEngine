package me.anno.tests.ui

import me.anno.config.DefaultConfig.style
import me.anno.engine.ui.AnyArrayPanel2
import me.anno.engine.ui.ComponentUI
import me.anno.engine.ui.ComponentUI.toTypedArray2
import me.anno.io.ISaveable.Companion.getReflections
import me.anno.studio.Inspectable
import me.anno.studio.InspectableProperty
import me.anno.ui.debug.TestStudio.Companion.testUI3

// todo implement editing huge arrays,
//  and refine our implementation to create subfolders like Chrome Debug Panel

// todo shift-clicking to collapse/uncollapse an item should work without selecting it first
// todo shift shouldn't be needed to collapse/uncollapse

// todo delete key is doing nothing...

// todo we need to show the actual, editable values

fun main() {
    testUI3("Edit Huge Arrays") {
        class Data : Inspectable {
            var data = IntArray(43) { it * it }
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
                property[this] = values.toTypedArray2(value)
            }
        }.apply {
            base = 2
            setValues(value.toList())
        }
    }
}