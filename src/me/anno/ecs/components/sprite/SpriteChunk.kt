package me.anno.ecs.components.sprite

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.ui.base.text.TextPanel.Companion.i0
import org.joml.Vector2i

class SpriteChunk(val key: Vector2i, var values: IntArray) : Saveable() {

    @Suppress("unused") // used for serialization
    constructor() : this(Vector2i(), i0)

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeVector2i("key", key)
        writer.writeIntArray("values", values)
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "key" -> key.set(value as? Vector2i ?: return)
            "values" -> values = value as? IntArray ?: return
            else -> super.setProperty(name, value)
        }
    }

    override fun clone(): SpriteChunk {
        return SpriteChunk(key, values.copyOf())
    }
}