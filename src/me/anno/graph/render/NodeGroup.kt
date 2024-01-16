package me.anno.graph.render

import me.anno.ecs.prefab.PrefabSaveable
import me.anno.graph.Node
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.engine.serialization.SerializedProperty
import org.joml.Vector3d

class NodeGroup : PrefabSaveable() {

    @SerializedProperty
    var color = 0

    @SerializedProperty
    val position = Vector3d()

    @SerializedProperty
    val extends = Vector3d()

    @SerializedProperty
    val members = ArrayList<Node>()

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeColor("color", color)
        writer.writeVector3d("position", position)
        writer.writeVector3d("extends", extends)
        writer.writeObjectList(null, "members", members)
    }

    override fun readInt(name: String, value: Int) {
        if (name == "color") color = value
        else super.readInt(name, value)
    }

    override fun readVector3d(name: String, value: Vector3d) {
        when (name) {
            "position" -> position.set(value)
            "extends" -> extends.set(value)
            else -> super.readVector3d(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        if (name == "members") {
            members.clear()
            members.addAll(values.filterIsInstance<Node>())
        } else super.readObjectArray(name, values)
    }
}