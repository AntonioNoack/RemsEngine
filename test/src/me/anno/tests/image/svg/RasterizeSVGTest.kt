package me.anno.tests.image.svg

import me.anno.image.svg.SVGMesh
import me.anno.image.svg.SVGRasterizer.rasterize
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.OS.desktop
import me.anno.utils.OS.res

/**
 * rasterize an SVG on the CPU,
 *   for emojis, SVG -> GPU -> CPU -> GPU is kind of wasteful
 * */
fun main() {
    val source = res.getChild("files/twemoji-1f1e6-1f1f8.svg")
    val svg = SVGMesh(XMLReader(source.inputStreamSync().reader()).readXMLNode()!!)
    for (size in listOf(64, 256, 1024)) {
        val image = svg.rasterize(size, size)
        image.write(desktop.getChild(source.getNameWithExtension("$size.png")))
    }
}