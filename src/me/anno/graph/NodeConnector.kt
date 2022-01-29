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

    // todo instead we could define a type, and let the graph ui render them
    // todo we could have a start and end color
    var color = -1

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

    open fun invalidate() {

    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "others", others)
        if (color != -1) writer.writeInt("color", color, true)
        writer.writeString("type", type)
        writer.writeSomething(this, "value", value, true)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "others" -> others = values.filterIsInstance<NodeConnector>()
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "color" -> color = value
            else -> super.readInt(name, value)
        }
    }

    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}