package me.anno.ecs.components.mesh.spline

import me.anno.io.base.BaseWriter
import me.anno.io.saveable.Saveable
import me.anno.maths.Maths.clamp
import me.anno.mesh.Triangulation
import me.anno.utils.Color.mixARGB
import me.anno.utils.Color.white
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2f
import kotlin.math.min

/**
 * e.g. main lanes + bus lane & tram lane + pedestrian path
 * */
class PathProfile() : Saveable() {

    constructor(pos: List<Vector2f>, uvs: FloatArrayList?, colors: IntArrayList?, isClosed: Boolean) : this() {
        positions = pos
        this.uvs = uvs
        this.colors = colors
        this.isClosed = isClosed
    }

    // todo add points
    // todo points: position (x,y) and color (r,g,b)

    // todo generate profile from mesh

    // base width
    var width = 1f

    var positions: List<Vector2f> = emptyList()
    var uvs: FloatArrayList? = null
    var colors: IntArrayList? = null

    var flatShading = true

    var isClosed = false

    var validTris = false
    var tris: List<Vector2f>? = null

    var mixColors = false

    fun getFacade(): List<Vector2f>? {
        if (!validTris || tris == null) {
            tris = Triangulation.ringToTrianglesVec2f(positions)
        }
        return tris
    }

    fun getSize(): Int {
        val colors = colors ?: return positions.size
        return min(positions.size, colors.size)
    }

    fun getSize2(): Int {
        val size = getSize()
        return if (isClosed) size + 1 else size
    }

    fun getPosition(i: Int): Vector2f {
        val j = if (isClosed) (i + positions.size) % positions.size
        else clamp(i, 0, positions.lastIndex)
        return positions[j]
    }

    fun getNormal(i: Int, end: Boolean, dst: Vector2f = Vector2f()): Vector2f {
        val j = if (end && !flatShading) i + 1 else i
        if (flatShading) {
            val v0 = getPosition(j)
            val v1 = getPosition(j + 1)
            dst.set(v1).sub(v0)
        } else {
            val v0 = getPosition(j - 1)
            val v1 = getPosition(j + 1)
            dst.set(v1).sub(v0)
        }
        dst.set(-dst.y, dst.x)
        return dst
    }

    /**
     * split profile into <0/>=0
     * */
    fun split(): Pair<PathProfile, PathProfile> {
        val positions = positions
        val colors = colors
        val cap = positions.size
        val left = ArrayList<Vector2f>(cap)
        val leftColors = IntArrayList(cap)
        val right = ArrayList<Vector2f>(cap)
        val rightColors = IntArrayList(cap)
        val isClosed = isClosed
        val i0 = if (isClosed) cap - 1 else 0
        var p0 = positions[i0]
        var c0 = colors?.getOrNull(i0) ?: white
        // add first point
        val sx = p0.x < 0f
        if (sx) {
            left.add(p0)
            leftColors.add(c0)
        } else {
            right.add(p0)
            rightColors.add(c0)
        }
        // add all segments to their respective side
        for (i in (if (isClosed) 0 else 1) until cap) {
            val p1 = positions[i]
            val c1 = colors?.getOrNull(i) ?: white
            val s0 = p0.x <= 0f
            val s1 = p1.x <= 0f
            if (s0 == s1) {
                // simple
                if (s0) {
                    left.add(p1)
                    leftColors.add(c1)
                } else {
                    right.add(p1)
                    rightColors.add(c1)
                }
            } else {
                // split segment
                val f = p0.x / (p0.x - p1.x)
                val pf = Vector2f(p0).lerp(p1, f)
                val cf = mixARGB(c0, c1, f)
                if (s0) {
                    // first left, then right
                    left.add(pf)
                    leftColors.add(cf)
                    // todo add marker that it ends / start here
                    right.add(pf)
                    rightColors.add(cf)
                    right.add(p1)
                    rightColors.add(c1)
                } else {
                    // first right, then left
                    right.add(pf)
                    rightColors.add(cf)
                    // todo add marker that it ends / starts here
                    left.add(pf)
                    leftColors.add(cf)
                    left.add(p1)
                    leftColors.add(c1)
                }
            }
            p0 = p1
            c0 = c1
        }
        val left1 = PathProfile(left, null, leftColors, false)
        val right1 = PathProfile(right, null, rightColors, false)
        left1.flatShading = flatShading
        right1.flatShading = flatShading
        return Pair(left1, right1)
    }

    fun getColor(i: Int, first: Boolean): Int {
        val colors = colors ?: return -1
        val index = if (mixColors) {
            (i + 1 - first.toInt()) % colors.size
        } else {
            i % colors.size
        }
        return colors[index]
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeFloat("width", width)
        writer.writeBoolean("flatShading", flatShading)
        writer.writeBoolean("isClosed", isClosed)
        writer.writeVector2fList("positions", positions)
        val colors = colors
        if (colors != null) {
            writer.writeIntArray("colors", colors.toIntArray())
        }
    }

    override fun setProperty(name: String, value: Any?) {
        when (name) {
            "width" -> width = value as? Float ?: return
            "flatShading" -> flatShading = value == true
            "isClosed" -> isClosed = value == true
            "positions" -> {
                val values = value as? List<*> ?: return
                positions = values.filterIsInstance<Vector2f>()
            }
            "colors" -> {
                val values = value as? IntArray ?: return
                colors = IntArrayList(values)
            }
            else -> super.setProperty(name, value)
        }
    }
}