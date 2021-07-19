package me.anno.ecs.prefab

import me.anno.ecs.Component

class ChangeSetComponentAttribute : ChangeSetAttribute {
    constructor(value: Any?) : super(value, 4)
    constructor() : super(null, 4)
    constructor(path: Path, value: Any?) : this(value) {
        this.path = path
    }

    override fun applyChange(element: Any?, name: String?) {
        element as Component
        when (name) {
            "name" -> element.name = value.toString()
            "description", "desc" -> element.description = value.toString()
            "isEnabled" -> element.isEnabled = value as? Boolean ?: return
            null -> {
            }
            else -> element[name] = value
        }
    }

    override val className: String = "ChangeSetComponentAttribute"
}
