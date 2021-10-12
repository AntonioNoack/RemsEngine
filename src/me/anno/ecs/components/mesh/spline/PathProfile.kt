package me.anno.ecs.components.mesh.spline

import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import org.joml.Vector2f
import kotlin.math.max
import kotlin.math.min

/**
 * e.g. main lanes + bus lane & tram lane + pedestrian path
 * */
class PathProfile() : Saveable() {

    constructor(pos: List<Vector2f>, colors: IntArray, isClosed: Boolean) : this() {
        positions = pos
        this.colors = colors.toList()
        this.isClosed = isClosed
    }

    // todo add points
    // todo points: position (x,y) and color (r,g,b)

    // todo generate profile from mesh

    // base width
    var width = 1f

    var positions: List<Vector2f> = emptyList()
    var colors: List<Int> = emptyList()

    var flatShading = true

    var isClosed = false

    fun getSize(): Int {
        return min(positions.size, colors.size)
    }

    fun getSize2(): Int {
        val size = getSize()
        return if (isClosed) size + 1 else size
    }

    fun getPosition(i: Int): Vector2f {
        return positions[i % positions.size]
    }

    fun getNormal(i: Int, end: Boolean, dst: Vector2f = Vector2f()): Vector2f {
        val j = if (end && !flatShading) i + 1 else i
        // todo if closed, wrap around
        if (flatShading) {
            val v0 = getPosition(j)
            val v1 = getPosition(j + 1)
            dst.set(v1).sub(v0)
        } else {
            val v0 = getPosition(max(j - 1, 0))
            val v1 = getPosition(j + 1)
            dst.set(v1).sub(v0)
        }
        dst.set(-dst.y, dst.x)
        return dst
    }

    fun getColor(i: Int): Int {
        return colors[i % colors.size]
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("width", width)
        writer.writeBoolean("flatShading", flatShading)
        writer.writeBoolean("isClosed", isClosed)
        writer.writeVector2fArray("positions", positions.toTypedArray())
        writer.writeIntArray("colors", colors.toIntArray())
    }

    override fun readFloat(name: String, value: Float) {
        when (name) {
            "width" -> width = value
            else -> super.readFloat(name, value)
        }
    }

    override fun readBoolean(name: String, value: Boolean) {
        when (name) {
            "flatShading" -> flatShading = value
            "isClosed" -> isClosed = false
            else -> super.readBoolean(name, value)
        }
    }

    override fun readVector2fArray(name: String, values: Array<Vector2f>) {
        if (name == "positions") positions = values.toList()
        else super.readVector2fArray(name, values)
    }

    override fun readIntArray(name: String, values: IntArray) {
        if (name == "colors") colors = values.toList()
        else super.readIntArray(name, values)
    }

}