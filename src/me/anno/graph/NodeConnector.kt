package me.anno.graph

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import org.joml.Vector3d

abstract class NodeConnector : NamedSaveable {

    constructor() : super()
    constructor(type: String) : super() {
        this.type = type
    }

    constructor(type: String, node: Node) : this(type) {
        this.node = node
    }

    constructor(type: String, name: String, node: Node) : this(type, node) {
        this.name = name
    }

    var type = "Any?"

    var value: Any? = null

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

    var node: Node? = null
    var others: List<NodeConnector> = emptyList()

    val connectorPosition = Vector3d()

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
        others.forEach { o -> o.others = o.others.filter { it != this } }
        others = emptyList()
    }

    operator fun contains(other: NodeConnector): Boolean {
        return other in others
    }

    open fun invalidate() {

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "node", node)
        writer.writeObjectList(this, "others", others)
        writer.writeString("type", type)
        writer.writeSomething(this, "value", value, true)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "node" -> node = value as? Node
            else -> super.readObject(name, value)
        }
    }

    override fun readSomething(name: String, value: Any?) {
        when (name) {
            "value" -> this.value = value
            else -> super.readSomething(name, value)
        }
    }

    override fun readString(name: String, value: String?) {
        when (name) {
            "type" -> type = value ?: type
            else -> super.readString(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "others" -> others = values.filterIsInstance<NodeConnector>()
            else -> super.readObjectArray(name, values)
        }
    }

    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}