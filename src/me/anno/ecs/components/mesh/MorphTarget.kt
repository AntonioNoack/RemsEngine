package me.anno.ecs.components.mesh

import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter

// normals as well?
/**
 * not yet properly supported!!!
 * */
class MorphTarget(name: String, var positions: FloatArray, var weight: Float = 0f) : NamedSaveable() {

    constructor() : this("", FloatArray(0))

    init {
        this.name = name
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloatArray("positions", positions)
        writer.writeFloat("weight", weight)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "weight" -> weight = value as? Float ?: return
            "positions" -> positions = value as? FloatArray ?: return
            else -> super.setProperty(name, value)
        }
    }

    override val approxSize get() = 5
}