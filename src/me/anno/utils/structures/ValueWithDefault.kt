package me.anno.utils.structures

import me.anno.io.saveable.Saveable
import me.anno.io.base.BaseWriter

/**
 * class for values, which have not the typical default value
 * these values don't need to be saved in text form,
 * because they can be set automatically
 * */
@Suppress("unused") // is used lots in Rem's Studio
class ValueWithDefault<V>(
    private var state: V?,
    private var default: V
) {

    constructor(value: V) : this(null, value)

    var wasSet = false
    val isSet get() = state != null && (state != default || wasSet)
    fun write(writer: BaseWriter, self: Saveable?, name: String) {
        if (isSet) {
            writer.writeSomething(self, name, state, true)
        }
    }

    var value
        get() = if (isSet) state!! else default
        set(value) {
            state = value
            wasSet = true
        }

    fun reset() {
        wasSet = false
        state = null
    }

    fun setDefault(v: V) {
        default = v
        state = null
    }

    companion object {
        @Suppress("unused") // used in Rem's Studio
        fun BaseWriter.writeMaybe(self: Saveable?, name: String, value: ValueWithDefault<*>) {
            value.write(this, self, name)
        }

        @Suppress("unused") // used in Rem's Studio
        fun BaseWriter.writeMaybe(self: Saveable?, name: String, value: ValueWithDefaultFunc<*>) {
            value.write(this, self, name)
        }
    }
}