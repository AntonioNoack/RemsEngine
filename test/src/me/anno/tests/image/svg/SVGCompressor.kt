package me.anno.tests.image.svg

import me.anno.image.svg.applyTransform
import me.anno.image.svg.readSVGPath
import me.anno.io.files.FileReference
import me.anno.io.xml.ComparableStringBuilder
import me.anno.io.xml.XMLNode
import me.anno.io.xml.XMLReader
import me.anno.maths.Maths.sq
import me.anno.utils.OS.downloads
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix3x2f
import kotlin.math.*

fun main() {
    // parse everything absolute
    // write everything with the shortest form
    val src = downloads.listChildren()!!
    val dst = downloads.getChild("compressed")
    for (s in src) {
        if (s.lcExtension == "svg") {
            s.nameWithoutExtension.toIntOrNull() ?: continue
            compressSVG(s, dst.getChild(s.name))
        }
    }
}

fun compressSVG(src: FileReference, dst: FileReference) {
    val svg = XMLReader().parse(src.inputStreamSync()) as XMLNode
    val viewBox = svg["viewBox"]!!.split(' ').map { it.toFloat() }
    val bld = ComparableStringBuilder(src.length().toInt())
    val dw = 999
    val dh = 999
    val dx = viewBox[0]
    val dy = viewBox[1]
    val sc = min(dw / viewBox[2], dh / viewBox[3])
    bld.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 $dw $dh\">")

    fun f(f: Float) = f.roundToInt()

    fun String.shorten(): String {
        return replace(" -", "-")
            .replace("0.0 ", "0 ")
    }

    var ll0 = 0
    var ll1 = 0
    var dx0 = 0
    var dy0 = 0

    fun append(s: Char, s1: String, s2: String) {
        val s1x = s1.shorten()
        val s2x = s2.shorten()
        if (s1x.length <= s2x.length) {
            bld.append(s.uppercase()).append(s1x)
        } else {
            bld.append(s.lowercase()).append(s2x)
        }
    }

    fun appendLine(s1: String, s2: String, dx: Int, dy: Int) {
        if (s2 == "0 0") return
        val s1x = s1.shorten()
        val s2x = s2.shorten()
        if (s1x.length <= s2x.length) {
            bld.append('L').append(s1x)
        } else {
            if (ll1 > 0 && ll1 == bld.length &&
                abs(
                    atan2(dx.toFloat(), dy.toFloat()) -
                            atan2(dx0.toFloat(), dy0.toFloat())
                ) < 0.1f
            ) {
                bld.length = ll0
                dx0 += dx
                dy0 += dy
                bld.append("$dx0 $dy0".shorten())
            } else {
                ll0 = bld.length + 1
                bld.append('l').append(s2x)
                dx0 = dx
                dy0 = dy
            }
            ll1 = bld.length
        }
    }

    fun append(s: Char, s1: Any, s2: Any) {
        append(s, s1.toString(), s2.toString())
    }

    // todo if there is multiple cubic elements in a line, and generally the same direction, compress them

    val tr = Matrix3x2f()
    val stack = ArrayList<Matrix3x2f>()
    tr.translate(-dx, -dy).scale(sc)

    fun Matrix3x2f.rx(x: Float, y: Float) = m00 * x + m10 * y
    fun Matrix3x2f.ry(x: Float, y: Float) = m01 * x + m11 * y
    fun Matrix3x2f.ax(x: Float, y: Float) = rx(x, y) + m20
    fun Matrix3x2f.ay(x: Float, y: Float) = ry(x, y) + m21
    fun Matrix3x2f.rx(x: Float) = m00 * x
    fun Matrix3x2f.ry(y: Float) = m11 * y
    fun Matrix3x2f.ax(x: Float) = rx(x) + m20
    fun Matrix3x2f.ay(y: Float) = ry(y) + m21

    fun attr(k: String, v: String) {
        bld.append(' ').append(k).append("=\"").append(v).append("\"")
    }

    fun attr(xml: XMLNode, filter: (String) -> Boolean) {
        for ((k, v) in xml.attributes) {
            if (filter(k)) attr(k, v)
        }
    }

    fun appendPath(xml: XMLNode) {
        bld.append("<path")
        attr(xml) { it != "d" }
        bld.append(" d=\"")
        var x = 0f
        var y = 0f
        readSVGPath(xml["d"]!!, { bld.append('z') }, { s, vi ->
            when (s) {
                'H' -> { // absolute
                    val v = tr.ax(vi, 0f)
                    append(s, f(v), f(v - x))
                    x = v
                }
                'h' -> { // relative
                    val v = x + tr.rx(vi, 0f)
                    append(s, f(v), f(v - x))
                    x = v
                }
                'V' -> { // absolute
                    val v = tr.ay(0f, vi)
                    append(s, f(v), f(v - y))
                    y = v
                }
                'v' -> { // relative
                    val v = y + tr.ry(0f, vi)
                    append(s, f(v), f(v - y))
                    y = v
                }
            }
        }, { s, x0i, y0i ->
            val x0: Float
            val y0: Float
            if (s.isUpperCase()) {
                x0 = tr.ax(x0i, y0i)
                y0 = tr.ay(x0i, y0i)
            } else {
                x0 = x + tr.rx(x0i, y0i)
                y0 = y + tr.ry(x0i, y0i)
            }
            if (s == 'L' || s == 'l') {
                appendLine("${f(x0)} ${f(y0)}", "${f(x0 - x)} ${f(y0 - y)}", f(x0 - x), f(y0 - y))
            } else {
                append(s, "${f(x0)} ${f(y0)}", "${f(x0 - x)} ${f(y0 - y)}")
            }
            x = x0
            y = y0
        }, { s, x0i, y0i, x1i, y1i ->
            val x0: Float
            val y0: Float
            val x1: Float
            val y1: Float
            if (s.isUpperCase()) {
                x0 = tr.ax(x0i, y0i)
                y0 = tr.ay(x0i, y0i)
                x1 = tr.ax(x1i, y1i)
                y1 = tr.ay(x1i, y1i)
            } else {
                x0 = x + tr.rx(x0i, y0i)
                y0 = y + tr.ry(x0i, y0i)
                x1 = x + tr.rx(x1i, y1i)
                y1 = y + tr.ry(x1i, y1i)
            }
            append(
                s, "${f(x0)} ${f(y0)} ${f(x1)} ${f(y1)}",
                "${f(x0 - x)} ${f(y0 - y)} ${f(x1 - x)} ${f(y1 - y)}"
            )
            x = x1
            y = y1
        }, { s, x0i, y0i, x1i, y1i, x2i, y2i ->
            // cubic
            val x0: Float
            val y0: Float
            val x1: Float
            val y1: Float
            val x2: Float
            val y2: Float
            if (s.isUpperCase()) {
                x0 = tr.ax(x0i, y0i)
                y0 = tr.ay(x0i, y0i)
                x1 = tr.ax(x1i, y1i)
                y1 = tr.ay(x1i, y1i)
                x2 = tr.ax(x2i, y2i)
                y2 = tr.ay(x2i, y2i)
            } else {
                x0 = x + tr.rx(x0i, y0i)
                y0 = y + tr.ry(x0i, y0i)
                x1 = x + tr.rx(x1i, y1i)
                y1 = y + tr.ry(x1i, y1i)
                x2 = x + tr.rx(x2i, y2i)
                y2 = y + tr.ry(x2i, y2i)
            }
            val l = 100f
            if (sq(x0 - x, y0 - y) < l &&
                sq(x1 - x, y1 - y) < l &&
                sq(x2 - x, y2 - y) < l
            ) { // short -> replace with line
                appendLine(
                    "${f(x2)} ${f(y2)}",
                    "${f(x2 - x)} ${f(y2 - y)}", f(x2 - x), f(y2 - y)
                )
            } else {
                append(
                    s, "${f(x0)} ${f(y0)} ${f(x1)} ${f(y1)} ${f(x2)} ${f(y2)}",
                    "${f(x0 - x)} ${f(y0 - y)} ${f(x1 - x)} ${f(y1 - y)} ${f(x2 - x)} ${f(y2 - y)}"
                )
            }
            x = x2
            y = y2
        }, { s, rxi, ryi, rot, lai, swi, x0i, y0i ->
            val x0: Float
            val y0: Float
            if (s.isUpperCase()) {
                x0 = tr.ax(x0i, y0i)
                y0 = tr.ay(x0i, y0i)
            } else {
                x0 = x + tr.rx(x0i, y0i)
                y0 = y + tr.ry(x0i, y0i)
            }
            val rx = f(tr.rx(rxi, ryi))
            val ry = f(tr.ry(rxi, ryi))
            val la = lai.toInt()
            val sw = swi.toInt()
            append(
                s,
                "$rx $ry $rot $la $sw ${f(x0)} ${f(y0)}",
                "$rx $ry $rot $la $sw ${f(x0 - x)} ${f(y0 - y)}"
            )
            x = x0
            y = y0
        })
        bld.append("\"/>")
    }

    fun process(xml: Any?) {
        when ((xml as? XMLNode)?.type) {
            "path" -> appendPath(xml)
            "g" -> {
                val trI = xml["transform"]
                val group = trI != null && trI.contains("rotate")
                val attr = xml.attributes.any { it.key != "transform" } || group
                if (group) {
                    bld.append("<g transform=\"$trI\"")
                } else if (trI != null) {
                    // save
                    stack.add(Matrix3x2f(tr))
                    applyTransform(tr, trI)
                }
                if (attr) {
                    if(!group) bld.append("<g")
                    attr(xml) { it != "transform" }
                    bld.append(">")
                }
                for (child in xml.children) {
                    process(child)
                }
                if (attr) bld.append("</g>")
                if (!group && trI != null) {
                    // restore
                    tr.set(stack.removeLast())
                }
            }
            "polygon" -> {
                val points = xml["points"]!!.split(' ')
                    .filter { ',' in it }.map {
                        val s = it.split(',')
                        val xi = s[0].toFloat()
                        val yi = s[1].toFloat()
                        "${tr.ax(xi, yi)},${tr.ay(xi, yi)}"
                    }.joinToString(" ")
                bld.append("<polygon points=\"").append(points).append("\"")
                attr(xml) { it != "points" }
                bld.append("/>")
            }
            "rect" -> {
                val rx = f(tr.rx(max(xml["rx"]?.toFloat() ?: 0f, 0f)))
                val ry = f(tr.ry(max(xml["ry"]?.toFloat() ?: 0f, 0f)))

                val x = f(tr.ax(xml["x"]?.toFloat() ?: 0f))
                val y = f(tr.ay(xml["y"]?.toFloat() ?: 0f))
                val w = f(tr.rx(xml["width"]!!.toFloat()))
                val h = f(tr.ry(xml["height"]!!.toFloat()))

                if (w > 0 && h > 0) {
                    bld.append("<rect x=\"$x\" y=\"$y\" width=\"$w\" height=\"$h\"")
                    if (rx != 0) bld.append(" rx=\"$rx\"")
                    if (ry != 0) bld.append(" ry=\"$ry\"")
                    attr(xml) {
                        when (it) {
                            "x", "y", "rx", "ry", "width", "height" -> false
                            else -> true
                        }
                    }
                    bld.append("/>")
                }
            }
            "circle" -> {
                val r = f(abs(tr.rx(xml["r"]!!.toFloat())))
                if (r > 0) {
                    val cxi = xml["cx"]!!.toFloat()
                    val cyi = xml["cy"]!!.toFloat()
                    val cx = f(tr.ax(cxi, cyi))
                    val cy = f(tr.ay(cxi, cyi))
                    bld.append("<circle cx=\"$cx\" cy=\"$cy\" r=\"$r\"")
                    attr(xml) {
                        when (it) {
                            "cx", "cy", "r" -> false
                            else -> true
                        }
                    }
                    bld.append("/>")
                }
            }
            "ellipse" -> {
                val rx = f(abs(tr.rx(xml["rx"]!!.toFloat())))
                val ry = f(abs(tr.ry(xml["ry"]!!.toFloat())))
                if (rx > 0 && ry > 0) {
                    val cxi = xml["cx"]!!.toFloat()
                    val cyi = xml["cy"]!!.toFloat()
                    val cx = f(tr.ax(cxi, cyi))
                    val cy = f(tr.ay(cxi, cyi))
                    bld.append("<ellipse cx=\"$cx\" cy=\"$cy\" rx=\"$rx\" ry=\"$ry\"")
                    attr(xml) {
                        when (it) {
                            "cx", "cy", "rx", "ry" -> false
                            else -> true
                        }
                    }
                    bld.append("/>")
                }
            }
            else -> {
                println("Unknown: $xml from $src")
                // bld.append(xml)
            }
        }
    }

    for (element in svg.children) {
        process(element)
    }
    bld.append("</svg>")
    dst.writeText(
        bld.toString()
            .replace("l0 0", "")
    )
}