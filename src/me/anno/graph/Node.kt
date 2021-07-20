package me.anno.graph

import me.anno.io.ISaveable
import me.anno.io.NamedSaveable
import me.anno.io.base.BaseWriter
import me.anno.ui.base.groups.PanelListY
import me.anno.ui.style.Style
import org.joml.Vector3d

abstract class Node : NamedSaveable() {

    abstract fun createUI(list: PanelListY, style: Style)

    val position = Vector3d()

    // multiple layers would be great for large functions :D
    // even though they really should be split...
    // but we may zoom into other functions :)
    var layer = 0

    var inputs: Array<NodeConnector>? = null
    var outputs: Array<NodeConnector>? = null

    abstract fun canAddInput(): Boolean
    abstract fun canAddOutput(): Boolean
    abstract fun canRemoveInput(): Boolean
    abstract fun canRemoveOutput(): Boolean

    // the node ofc needs to save its custom content and behaviour as well
    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectArray(this as ISaveable, "inputs", inputs)
        writer.writeObjectArray(this as ISaveable, "outputs", outputs)
        writer.writeInt("layer", layer)
        writer.writeVector3d("position", position)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "inputs" -> inputs = values.filterIsInstance<NodeConnector>().toTypedArray()
            "outputs" -> outputs = values.filterIsInstance<NodeConnector>().toTypedArray()
            else -> super.readObjectArray(name, values)
        }
    }

    override fun readInt(name: String, value: Int) {
        when (name) {
            "layer" -> layer = value
            else -> super.readInt(name, value)
        }
    }

    override fun readVector3d(name: String, value: Vector3d) {
        when (name) {
            "position" -> position.set(value)
            else -> super.readVector3d(name, value)
        }
    }

}