package me.anno.bullet.constraints

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.utils.types.AnyToFloat.getFloat

class ConstraintLimit(var dir: Float, var limit: Float, var orthogonal: Float) : Saveable() {

    constructor(value: Float) : this(value, value, value)

    fun set(src: ConstraintLimit) {
        dir = src.dir
        limit = src.limit
        orthogonal = src.orthogonal
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("dir", dir, true)
        writer.writeFloat("limit", limit, true)
        writer.writeFloat("orthogonal", orthogonal, true)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "dir" -> dir = getFloat(value)
            "limit" -> limit = getFloat(value)
            "orthogonal" -> orthogonal = getFloat(value)
            else -> super.setProperty(name, value)
        }
    }
}