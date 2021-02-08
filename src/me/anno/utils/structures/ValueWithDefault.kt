package me.anno.utils.structures

import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.objects.animation.TimeValue.Companion.writeValue

/**
 * class for values, which have not the typical default value
 * these values don't need to be saved in text form,
 * because they can be set automatically
 * */
class ValueWithDefault<V>(
    private var state: V,
    private var default: V
) {
    constructor(value: V) : this(value, value)

    var wasSet = false
    val isSet get() = state != default && wasSet
    fun write(writer: BaseWriter, self: ISaveable?, name: String) {
        if (isSet) {
            writer.writeValue(self, name, state)
        }
    }

    var value
        get() = if (wasSet) state else default
        set(value) {
            state = value
            wasSet = true
        }

    fun reset() {
        wasSet = false
    }

    fun setDefault(v: V) {
        state = v
        default = v
    }

    /**
     * sets the value for a slightly cleaner look
     * (even if awkward)
     * can be misleading in the context of numbers...
     * */
    operator fun timesAssign(value: V){
        this.value = value
    }

    companion object {
        fun BaseWriter.writeMaybe(self: ISaveable?, name: String, value: ValueWithDefault<*>) {
            value.write(this, self, name)
        }

        fun BaseWriter.writeMaybe(self: ISaveable?, name: String, value: ValueWithDefaultFunc<*>) {
            value.write(this, self, name)
        }
    }
}