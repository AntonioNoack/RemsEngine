package me.anno.graph

import me.anno.ecs.annotations.HideInInspector
import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import org.joml.Vector3d

abstract class NodeConnector(var isCustom: Boolean) : NamedSaveable() {

    constructor(type: String, isCustom: Boolean) : this(isCustom) {
        this.type = type
    }

    constructor(type: String, node: Node, isCustom: Boolean) : this(type, isCustom) {
        this.node = node
    }

    constructor(type: String, name: String, node: Node, isCustom: Boolean) : this(type, node, isCustom) {
        this.name = name
    }

    var type = "Any?"

    var currValue: Any? = null
    var defaultValue: Any? = null

    // todo special node ui? would help with the layouts :)
    // todo we could add input- and output panels there :)

    // should/would be set by the ui routine
    var position = Vector3d()

    var thickness = 0

    // todo display flowing pearls
    var isBeingUsed = 0f

    // maybe?...
    var isEnabled = false

    // we could define in-between points for better routing layouts
    // todo knots for better routing layouts

    @HideInInspector
    var node: Node? = null

    @HideInInspector
    var others: List<NodeConnector> = emptyList()

    fun isEmpty() = others.isEmpty()

    fun connect(other: NodeConnector) {
        others = others + other
        other.others += this
    }

    fun disconnect(other: NodeConnector) {
        other.others = other.others.filter { it != this }
        others = others.filter { it != other }
    }

    fun disconnectAll() {
        for (o in others) {
            o.others = o.others.filter { it != this }
        }
        others = emptyList()
    }

    operator fun contains(other: NodeConnector): Boolean {
        return other in others
    }

    open fun invalidate() {
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        // node is not necessarily needed
        // writer.writeObject(this, "node", node)

        writer.writeString("type", type)
        if (isCustom) writer.writeBoolean("custom", true)

        writer.writeObjectList(this, "others", others)
        if (currValue != null) {
            try {
                writer.writeSomething(this, "value", currValue, true)
            } catch (ignored: RuntimeException) {
                // may not be serializable
            }
        }
    }

    override fun readBoolean(name: String, value: Boolean) {
        if (name == "custom") isCustom = value
        else super.readBoolean(name, value)
    }

    override fun readObject(name: String, value: ISaveable?) {
        if (name == "node") node = value as? Node
        else super.readObject(name, value)
    }

    override fun readSomething(name: String, value: Any?) {
        if (name == "value") currValue = value
        else super.readSomething(name, value)
    }

    override fun readString(name: String, value: String) {
        if (name == "type") type = value
        else super.readString(name, value)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "others" -> others = values.filterIsInstance<NodeConnector>()
            else -> super.readObjectArray(name, values)
        }
    }
}