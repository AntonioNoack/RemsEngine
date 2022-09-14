package me.anno.image.svg.gradient

import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLElement
import me.anno.utils.types.Vectors.print
import org.joml.Vector2d

/**
Example:
<radialGradient id="hairHighlights_1_" cx="61.0759" cy="94.7296" r="23.3126"
gradientTransform="matrix(0.9867 -0.1624 -0.1833 -1.1132 16.8427 148.0534)" gradientUnits="userSpaceOnUse">
<stop  offset="0.7945" style="stop-color:#6D4C41;stop-opacity:0"/>
<stop  offset="1" style="stop-color:#6D4C41"/>
 * */
class RadialGradient(mesh: SVGMesh, xmlElement: XMLElement) : Gradient1D(xmlElement) {

    // https://www.w3.org/TR/SVG/pservers.html#RadialGradientElementFXAttribute

    val position = Vector2d()
    var r = 0.5

    // fx, fy, und fr sind irgendwie komplizierter...
    // kA, ob man die mit einem Polynom 2. Grades darstellen kann;
    // vermutlich nicht

    override fun fillFormula(formula: Formula) {
        val invR = 1 / r // why inverted???
        formula.position.set(0.5 - position.x, 0.5 - position.y)
        formula.directionOrRadius.set(invR)
        formula.isCircle = true
    }

    override fun toString(): String = "RadialGradient(${position.print()} $r)"

    init {
        parseStops(mesh, xmlElement.children)
        position.set(
            xmlElement["cx"]?.toDouble() ?: position.x,
            xmlElement["cy"]?.toDouble() ?: position.y
        )
        r = xmlElement["r"]?.toDouble() ?: r
    }

}