package me.anno.utils.structures

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter

/**
 * class for values, which have not the typical default value
 * these values don't need to be saved in text form,
 * because they can be set automatically
 * */
class ValueWithDefaultFunc<V>(
    private var state: V?, private var default: () -> V
) {
    constructor(value: V) : this(null, { value })
    constructor(value: () -> V) : this(null, value)

    var wasSet = false
    val isSet get() = state != null && (state != default() || wasSet)

    var value
        get() = if (isSet) state!! else default()
        set(value) {
            state = value
            wasSet = true
        }

    fun reset() {
        wasSet = false
        state = null
    }

    fun write(writer: BaseWriter, self: Saveable?, name: String) {
        if (isSet) {
            writer.writeSomething(self, name, state, true)
        }
    }

}