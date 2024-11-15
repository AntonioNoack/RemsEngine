package me.anno.games.flatworld.streets

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import org.joml.Vector3d

class StreetSegmentData : Saveable() {

    val a = Vector3d()
    val c = Vector3d()
    var b: Vector3d? = null

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val b = b
        writer.writeVector3d("a", a)
        if (b != null) writer.writeVector3d("b", b)
        writer.writeVector3d("c", c)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "a" -> a.set(value as? Vector3d ?: return)
            "c" -> c.set(value as? Vector3d ?: return)
            "b" -> b = value as? Vector3d ?: return
            else -> super.setProperty(name, value)
        }
    }
}