package me.anno.graph.visual.control

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToLong
import kotlin.math.abs

class ForLoopState(
    var currentIndex: Long, var endIndex: Long,
    var increment: Long,
) : Saveable() {
    @Suppress("unused") // needed for registering
    constructor() : this(0, 0, 0)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeLong("current", currentIndex)
        writer.writeLong("end", endIndex)
        writer.writeLong("increment", increment)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "current" -> currentIndex = AnyToLong.getLong(value)
            "end" -> endIndex = AnyToLong.getLong(value)
            "increment" -> increment = AnyToLong.getLong(value, 1)
            else -> super.setProperty(name, value)
        }
    }

    override fun toString(): String {
        val compSymbol = if (increment > 0) '<' else '>'
        val incSymbol = if (increment > 0) '+' else '-'
        return "ForLoopState[$currentIndex $compSymbol $endIndex; $incSymbol= ${abs(increment)}]"
    }
}