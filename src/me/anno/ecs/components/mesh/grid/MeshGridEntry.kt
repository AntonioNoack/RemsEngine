package me.anno.ecs.components.mesh.grid

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import org.joml.Vector2i

class MeshGridEntry(var position: Vector2i, var mesh: FileReference) : Saveable() {
    @Suppress("unused") // used by deserialization
    constructor() : this(Vector2i(), InvalidRef)

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "pos" -> position.set(value as? Vector2i ?: return)
            "grid" -> mesh = value as? FileReference ?: return
            else -> super.setProperty(name, value)
        }
    }
}