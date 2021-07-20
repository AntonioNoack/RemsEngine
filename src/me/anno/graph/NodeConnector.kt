package me.anno.graph

import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.Vector3d

class NodeConnector : Saveable() {

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
    var targetNode: Node? = null

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObject(this, "node", node)
        writer.writeInt("color", color)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "node" -> node = value as? Node
            else -> super.readObject(name, value)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "color" -> color = value
            else -> super.readInt(name, value)
        }
    }

    override val className: String = "NodeConnector"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}