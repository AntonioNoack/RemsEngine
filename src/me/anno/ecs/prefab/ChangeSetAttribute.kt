package me.anno.ecs.prefab

import me.anno.io.base.BaseWriter

abstract class ChangeSetAttribute(var value: Any?, priority: Int) : Change(priority) {

    constructor(priority: Int) : this(null, priority)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // force it to keep the order
        writer.writeSomething(null, "value", value, true)
    }

    override fun readSomething(name: String, value: Any?) {
        when (name) {
            "value" -> this.value = value
            else -> super.readSomething(name, value)
        }
    }

    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}
