package me.anno.image.svg.gradient

import me.anno.image.svg.SVGMesh
import me.anno.io.xml.generic.XMLNode
import me.anno.utils.types.AnyToFloat.getFloat
import org.joml.Vector2f

class LinearGradient(mesh: SVGMesh, xmlNode: XMLNode) : Gradient1D(xmlNode) {

    init {
        parseStops(mesh, xmlNode.children)
    }

    // gradient vector
    val p0 = Vector2f(getFloat(xmlNode["x1"], 0f), getFloat(xmlNode["y1"], 0f))
    val p1 = Vector2f(getFloat(xmlNode["x2"], 1f), getFloat(xmlNode["y2"], 0f))

    override fun fillFormula(formula: Formula) {
        val lengthSq = p1.distanceSquared(p0)
        formula.position.set(p0)
        formula.directionOrRadius.set(p1).sub(p0).div(lengthSq)
        formula.isCircle = false
    }

    override fun getProgress(x: Float, y: Float): Float {
        val lengthSq = p1.distanceSquared(p0)
        val dx = p1.x - p0.x
        val dy = p1.y - p0.y
        return ((x - p0.x) * dx + (y - p0.y) * dy) / lengthSq
    }

    override fun toString(): String = "LinearGradient($p0 $p1)"
}