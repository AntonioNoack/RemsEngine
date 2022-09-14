package me.anno.image.svg.gradient

import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLElement
import me.anno.parser.SimpleExpressionParser
import org.joml.Vector2d

class LinearGradient(mesh: SVGMesh, xmlElement: XMLElement) : Gradient1D(xmlElement) {

    // gradient vector
    val p0 = Vector2d()
    val p1 = Vector2d()

    init {
        parseStops(mesh, xmlElement.children)
        p0.set(parseFloat(xmlElement["x1"], 0.0), parseFloat(xmlElement["y1"], 0.0))
        p1.set(parseFloat(xmlElement["x2"], 1.0), parseFloat(xmlElement["y2"], 0.0))
    }

    private fun parseFloat(str: String?, default: Double): Double {
        if (str == null) return default
        return SimpleExpressionParser.parseDouble(str) ?: default
    }

    override fun fillFormula(formula: Formula) {
        val length = p1.distance(p0)
        val dif = Vector2d(p1).sub(p0)
        formula.position.set(p0)
        formula.directionOrRadius.set(dif).div(length * length)
        formula.isCircle = false
    }

    override fun toString(): String = "LinearGradient($p0 $p1)"

}