package me.anno.ecs.prefab

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.io.base.BaseWriter

class ChangeAddComponent() : Change(2) {

    var type: String = ""

    constructor(type: String) : this() {
        this.type = type
    }

    constructor(path: Path, type: String) : this() {
        this.type = type
        this.path = path
    }

    // name is not used -> could be used as type...

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("type", type)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "type" -> type = value
            else -> super.readString(name, value)
        }
    }

    override fun applyChange(element: Any?, name: String?) {
        element as Entity
        element.addComponent(Component.create(type))
    }

    override val className: String = "ChangeAddComponent"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false
}
