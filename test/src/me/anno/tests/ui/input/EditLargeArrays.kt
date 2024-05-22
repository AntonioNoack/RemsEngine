package me.anno.tests.ui.input

import me.anno.config.DefaultConfig.style
import me.anno.engine.inspector.Inspectable
import me.anno.engine.ui.input.AnyArrayPanel2
import me.anno.engine.ui.input.ComponentUI
import me.anno.engine.ui.input.ComponentUI.writeTo
import me.anno.io.saveable.Saveable.Companion.getReflections
import me.anno.ui.debug.TestEngine.Companion.testUI3

// some content seems to be missing / invisible: with base >= 15
// -> cannot reproduce it -> probably solved

fun main() {
    testUI3("Edit Huge Arrays") {
        class Data : Inspectable {
            var data = IntArray(4030) { it }
        }

        val instance = Data()
        val reflections = getReflections(instance)
        val property = reflections.allProperties["data"]!!
        val value = instance.data
        val arrayType = ComponentUI.getArrayType(value.iterator())!!
        object : AnyArrayPanel2("Data", "", arrayType, style) {
            override fun onChange() {
                property[instance] = values.writeTo(value)
            }
        }.apply {
            base = 16
            setValues(value.toList())
        }
    }
}