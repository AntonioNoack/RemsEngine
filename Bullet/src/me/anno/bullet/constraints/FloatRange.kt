package me.anno.bullet.constraints

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToFloat.getFloat

class FloatRange(var min: Float, var max: Float) : Saveable() {

    val isEmpty get() = min >= max

    operator fun contains(value: Float): Boolean = value in min..max

    fun set(other: FloatRange) = set(other.min, other.max)
    fun set(min: Float, max: Float): FloatRange {
        this.min = min
        this.max = max
        return this
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("min", min, true)
        writer.writeFloat("max", max, true)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "min" -> min = getFloat(value)
            "max" -> max = getFloat(value)
            else -> super.setProperty(name, value)
        }
    }
}