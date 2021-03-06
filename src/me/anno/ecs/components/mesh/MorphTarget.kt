package me.anno.ecs.components.mesh

import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter

// normals as well?
class MorphTarget(name: String, var positions: FloatArray, var weight: Float = 0f) : NamedSaveable() {

    constructor() : this("", FloatArray(0))

    init {
        this.name = name
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloatArray("positions", positions)
        writer.writeFloat("weight", weight) // todo is this the correct location for a weight? idk..
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "weight" -> weight = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readFloatArray(name: String, values: FloatArray) {
        when (name) {
            "positions" -> positions = values
            else -> super.readFloatArray(name, values)
        }
    }

    override val className: String = "MorphTarget"
    override val approxSize: Int = 5

}