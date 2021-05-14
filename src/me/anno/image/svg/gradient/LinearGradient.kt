package me.anno.image.svg.gradient

import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLElement
import me.anno.parser.SimpleExpressionParser
import org.joml.Vector2f

class LinearGradient : Gradient1D {

    // gradient vector
    val p0 = Vector2f()
    val p1 = Vector2f()

    constructor(): super()

    constructor(mesh: SVGMesh, xmlElement: XMLElement) : super(xmlElement) {
        parseStops(mesh, xmlElement.children)
        p0.set(parseFloat(xmlElement["x1"], 0f), parseFloat(xmlElement["y1"], 0f))
        p1.set(parseFloat(xmlElement["x2"], 1f), parseFloat(xmlElement["y2"], 0f))
    }

    private fun parseFloat(str: String?, default: Float): Float {
        if (str == null) return default
        return SimpleExpressionParser.parseDouble(str)?.toFloat() ?: default
    }

    override fun fillFormula(formula: Formula) {
        val length = p1.distance(p0)
        val dif = Vector2f(p1).sub(p0)
        val originProject = -dif.dot(p0) / (length * length)
        formula.v = originProject
        formula.x = dif.x / length
        formula.y = dif.y / length
        formula.xx = 0f
        formula.xy = 0f
        formula.yy = 0f
    }

    override fun toString(): String = "LinearGradient($p0 $p1)"

}