package me.anno.image.svg.gradient

import me.anno.image.svg.SVGMesh
import me.anno.io.xml.XMLElement

/**
Example:
<radialGradient id="hairHighlights_1_" cx="61.0759" cy="94.7296" r="23.3126"
gradientTransform="matrix(0.9867 -0.1624 -0.1833 -1.1132 16.8427 148.0534)" gradientUnits="userSpaceOnUse">
<stop  offset="0.7945" style="stop-color:#6D4C41;stop-opacity:0"/>
<stop  offset="1" style="stop-color:#6D4C41"/>
 * */
class RadialGradient : Gradient1D {

    // https://www.w3.org/TR/SVG/pservers.html#RadialGradientElementFXAttribute

    constructor() : super()

    constructor(mesh: SVGMesh, xmlElement: XMLElement) : super(xmlElement) {
        parseStops(mesh, xmlElement.children)
        cx = xmlElement["cx"]?.toFloat() ?: cx
        cy = xmlElement["cy"]?.toFloat() ?: cy
        r = xmlElement["r"]?.toFloat() ?: r
    }

    var cx = 0.5f
    var cy = 0.5f
    var r = 0.5f

    // fx, fy, und fr sind irgendwie komplizierter...
    // kA, ob man die mit einem Polynom 2. Grades darstellen kann;
    // vermutlich nicht

    override fun fillFormula(formula: Formula) {
        // todo this is awkward...
        formula.v = 0f
        formula.x = 0f
        formula.y = 0f
        formula.xx = 0.25f / (r * r)
        formula.yy = 0.25f / (r * r)
        formula.xy = 0f
        formula.translate(-cx, -cy)
    }

    override fun toString(): String = "RadialGradient($cx $cy $r)"

}