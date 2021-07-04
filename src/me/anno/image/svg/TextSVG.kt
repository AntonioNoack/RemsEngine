package me.anno.image.svg

import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader
import me.anno.utils.OS
import java.io.ByteArrayInputStream

fun main() {


    val text = "<svg height=\"100\" width=\"100\">\n" +
            "  <circle cx=\"50\" cy=\"50\" r=\"40\" stroke=\"black\" stroke-width=\"3\" fill=\"red\" />\n" +
            "<rect x=\"50\" y=\"20\" rx=\"20\" ry=\"20\" width=\"150\" height=\"150\" style=\"fill:red;stroke:black;stroke-width:5;opacity:0.5\" />" +
            "\n" +
            "  <ellipse cx=\"240\" cy=\"50\" rx=\"220\" ry=\"30\" style=\"fill:yellow\" />\n" +
            "  <ellipse cx=\"220\" cy=\"50\" rx=\"190\" ry=\"20\" style=\"fill:white\" />\n" +
            "  <polyline points=\"20,20 40,25 60,40 80,120 120,140 200,180\" style=\"fill:none;stroke:black;stroke-width:3\" />" +
            "  <path d=\"M159 112 A 126 47 17 1 0 292 108z\"/>" +
            "   Sorry, your browser does not support inline SVG.  \n" +
            "</svg> "

    testSVG(text)
    testSVG(OS.downloads.getChild("tiger.svg")!!.readText())

}

fun testSVG(text: String) {
    SVGMesh().parse(XMLReader.parse(ByteArrayInputStream(text.toByteArray())) as XMLElement)
}

fun testXML(text: String) {
    println(XMLReader.parse(ByteArrayInputStream(text.toByteArray())))
}