package me.anno.graph.visual.node

import me.anno.ecs.annotations.HideInInspector
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.NamedSaveable
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

abstract class NodeConnector(var isCustom: Boolean) : NamedSaveable() {

    companion object {
        private val LOGGER = LogManager.getLogger(NodeConnector::class)
    }

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
        if (currValue != null && currValue != InvalidRef) {
            try {
                writer.writeSomething(this, "value", currValue, true)
            } catch (e: RuntimeException) {
                // may not be serializable
                LOGGER.warn("$type/$name caused $e")
            }
        }
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "custom" -> isCustom = value == true
            "value" -> currValue = value
            "node" -> node = value as? Node
            "type" -> type = value as? String ?: return
            "others" -> others = (value as? List<*> ?: return).filterIsInstance<NodeConnector>()
            else -> super.setProperty(name, value)
        }
    }
}