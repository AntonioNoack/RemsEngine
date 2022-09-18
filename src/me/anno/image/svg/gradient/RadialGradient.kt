package me.anno.image.svg.gradient

import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLNode
import me.anno.maths.Maths.length
import me.anno.utils.types.Vectors.print
import org.joml.Vector2f

/**
 * https://www.w3.org/TR/SVG/pservers.html#RadialGradientElementFXAttribute
 *
 * Example:
 * <radialGradient id="hairHighlights_1_" cx="61.0759" cy="94.7296" r="23.3126"
 * gradientTransform="matrix(0.9867 -0.1624 -0.1833 -1.1132 16.8427 148.0534)" gradientUnits="userSpaceOnUse">
 * <stop  offset="0.7945" style="stop-color:#6D4C41;stop-opacity:0"/>
 * <stop  offset="1" style="stop-color:#6D4C41"/>
 * */
class RadialGradient(mesh: SVGMesh, xmlNode: XMLNode) : Gradient1D(xmlNode) {

    init {
        parseStops(mesh, xmlNode.children)
    }

    val position = Vector2f(
        xmlNode["cx"]?.toFloatOrNull() ?: 0f,
        xmlNode["cy"]?.toFloatOrNull() ?: 0f
    )

    val r = xmlNode["r"]?.toFloatOrNull() ?: 0.5f

    override fun fillFormula(formula: Formula) {
        formula.position.set(0.5f - position.x, 0.5f - position.y)
        formula.directionOrRadius.set(1f / r)
        formula.isCircle = true
    }

    override fun getProgress(x: Float, y: Float): Float {
        return length(x - (0.5f - position.x), y - (0.5f - position.y)) / r
    }

    override fun toString(): String = "RadialGradient(${position.print()} $r)"

}