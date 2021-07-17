package me.anno.ecs.prefab

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.io.NamedSaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.files.LocalFile.toGlobalFile
import org.joml.Quaterniond
import org.joml.Vector3d

// todo how do we reference (as variables) to other Entities? probably a path would be correct...
// todo the same for components

abstract class Change(val priority: Int) : Saveable() {

    var path: Path? = null

    fun apply(entity: Entity) {
        apply(entity, pathIndex = 0)
    }

    fun apply(entity: Entity, pathIndex: Int) {
        val path = path!!
        val delta = pathIndex - path.hierarchy.size
        if (delta < 0) {
            val childIndex = path.hierarchy[pathIndex]
            // we can go deeper :)
            if (delta == -1 && this is ChangeSetComponentValue) {
                // decide based on type
                applyChange(entity.components[childIndex], path.name)
            } else {
                // just go deeper
                apply(entity.children[childIndex], pathIndex + 1)
            }
        } else {
            applyChange(entity, path.name)
        }
    }

    abstract fun applyChange(element: Any?, name: String)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeString("path", path.toString())
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "path" -> path = Path.parse(value)
            else -> super.readString(name, value)
        }
    }

}

// never change the order -> we can use indices, and never have corruption, as long as the underlying mesh isn't changed

class ChangeAddEntity(var source: FileReference) : Change(0) {

    // doesn't need the name...

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFile("source", source)
    }

    override fun readString(name: String, value: String) {
        when (name) {
            "source" -> source = value.toGlobalFile()
            else -> super.readString(name, value)
        }
    }

    override fun applyChange(element: Any?, name: String) {
        element as Entity
        if (source == InvalidRef) {
            element.add(Entity())
        } else {
            TODO("create entity from $source")
        }
    }

    override val className: String = "ChangeAddEntity"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}

class ChangeAddComponent(var type: String) : Change(2) {

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

    override fun applyChange(element: Any?, name: String) {
        element as Entity
        element.addComponent(Component.create(type))
    }

    override val className: String = "ChangeAddComponent"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false
}

abstract class ChangeSetValue(var value: Any?, priority: Int) : Change(priority) {

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

    override fun applyChange(element: Any?, name: String) {
        element as NamedSaveable
        when (name) {
            "name" -> element.name = value.toString()
            "description" -> element.description = value.toString()
            "position" -> {
                if (element is Entity) {
                    element.transform.localPosition = value as? Vector3d ?: return
                } else element[name] = value
            }
            "rotation" -> {
                if (element is Entity) {
                    element.transform.localRotation = value as? Quaterniond ?: return
                } else element[name] = value
            }
            "scale" -> {
                if (element is Entity) {
                    element.transform.localScale = value as? Vector3d ?: return
                } else element[name] = value
            }
            else -> element[name] = value
        }
    }

    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}

class ChangeSetEntityValue : ChangeSetValue {
    constructor(value: Any?) : super(value, 3)
    constructor() : super(null, 3)

    override val className: String = "ChangeSetEntityValue"
}

class ChangeSetComponentValue : ChangeSetValue {
    constructor(value: Any?) : super(value, 4)
    constructor() : super(null, 4)

    override val className: String = "ChangeSetComponentValue"
}