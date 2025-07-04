package me.anno.graph.visual.render

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.serialization.SerializedProperty
import me.anno.graph.visual.node.Node
import me.anno.io.base.BaseWriter
import org.joml.Vector3d

class NodeGroup : PrefabSaveable() {

    @SerializedProperty
    var color = 0

    @SerializedProperty
    val position = Vector3d()

    @SerializedProperty
    val extents = Vector3d()

    @SerializedProperty
    val members = ArrayList<Node>()

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeColor("color", color)
        writer.writeVector3d("position", position)
        writer.writeVector3d("extents", extents)
        writer.writeObjectList(null, "members", members)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "color" -> color = value as? Int ?: return
            "position" -> position.set(value as? Vector3d ?: return)
            "extents" -> extents.set(value as? Vector3d ?: return)
            "members" -> {
                val values = value as? List<*> ?: return
                members.clear()
                members.addAll(values.filterIsInstance<Node>())
            }
            else -> super.setProperty(name, value)
        }
    }
}