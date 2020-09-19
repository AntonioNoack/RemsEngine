package me.anno.ui.custom.data

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.ui.base.Panel

abstract class CustomData: Saveable() {

    var weight = 1f

    abstract fun toPanel(isX: Boolean): Panel

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("weight", weight)
    }

    override fun readFloat(name: String, value: Float) {
        when(name){
            "weight" -> weight = value
            else -> super.readFloat(name, value)
        }
    }

    override fun getApproxSize() = 1
    override fun isDefaultValue() = false
}