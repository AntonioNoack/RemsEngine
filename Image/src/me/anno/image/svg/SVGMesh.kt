package me.anno.image.svg

import me.anno.config.DefaultConfig
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticBuffer
import me.anno.image.svg.SVGTransform.applyTransform
import me.anno.image.svg.gradient.Formula
import me.anno.image.svg.gradient.LinearGradient
import me.anno.image.svg.gradient.RadialGradient
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.io.yaml.generic.SimpleYAMLReader
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.unmix
import me.anno.utils.async.Callback
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4dArrayList
import org.joml.Vector2f
import org.joml.Vector4f
import org.the3deers.util.EarCut.pointInTriangle
import java.io.IOException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// todo create outline from svg? could be really nice to have :)

// to do animated svg
// to do transforms
// to do gradients
// to do don't use depth, use booleans on triangles to remove flickering

class SVGMesh {

    val stepsPerDegree = DefaultConfig["format.svg.stepsPerDegree", 0.1f]

    var z = 0f
    val deltaZ = 0.001f

    val styles = HashMap<String, Any>()

    // centered
    val bounds = AABBf()

    init {
        bounds.union(0f, 0f, 0f)
    }

    val transform = Matrix4dArrayList()

    val ids = HashMap<String, CSSData>()
    val classes = HashMap<String, CSSData>()

    fun parse(xml: XMLNode) {
        parseChildren(xml.children, null)
        val viewBox = (xml["viewBox"] ?: "0 0 100 100")
            .replace(',', ' ')
            .split(' ')
            .filter { !it.isBlank2() }
            .map { it.toFloat() }
        val w = viewBox[2]
        val h = viewBox[3]
        createMesh(viewBox[0], viewBox[1], w, h)
        createMesh2(viewBox[0], viewBox[1], w, h)
        bounds.minX = -w / (2f * h)
        bounds.maxX = +w / (2f * h)
        bounds.minY = -0.5f
        bounds.maxY = +0.5f
    }

    fun parseChildren(children: List<Any>, parentGroup: XMLNode?) {
        for (child in children) {
            child as? XMLNode ?: continue
            convertStyle(child)
            parentGroup?.attributes?.forEach { (key, value) ->
                if (key !in child.attributes) {
                    child[key] = value
                }
            }
            val style = SVGStyle(this, child)
            when (child.type.lowercase()) {
                "circle" -> {
                    if (style.isFill) addCircle(child, style, true)
                    if (style.isStroke) addCircle(child, style, false)
                }
                "rect" -> {
                    if (style.isFill) addRectangle(child, style, true)
                    if (style.isStroke) addRectangle(child, style, false)
                }
                "ellipse" -> {
                    if (style.isFill) addEllipse(child, style, true)
                    if (style.isStroke) addEllipse(child, style, false)
                }
                "line" -> {
                    if (style.isFill) addLine(child, style, true)
                }
                "polyline" -> {
                    if (style.isFill) addPolyline(child, style, true)
                    if (style.isStroke) addPolyline(child, style, false)
                }
                "polygon" -> {
                    if (style.isFill) addPolygon(child, style, true)
                    if (style.isStroke) addPolygon(child, style, false)
                }
                "path" -> {
                    if (style.isFill) addPath(child, style, true)
                    if (style.isStroke) addPath(child, style, false)
                }
                "g" -> {
                    val transform2 = child["transform"]
                    if (transform2 != null) {
                        transform.pushMatrix()
                        applyTransform(transform, transform2)
                    }
                    parseChildren(child.children, child)
                    if (transform2 != null) {
                        transform.popMatrix()
                    }
                }
                "switch", "foreignobject", "i:pgfref", "i:pgf", "defs" -> {
                    parseChildren(child.children, parentGroup)
                }
                "lineargradient" -> {
                    val id = child["id"]
                    if (id != null) {
                        /**
                        Example:
                        <linearGradient id="x" gradientUnits="userSpaceOnUse" x1="62.2648" y1="50.1708" x2="62.2648" y2="8.5885" gradientTransform="matrix(1 0 0 -1 0 128)">
                        <stop  offset="0" style="stop-color:#00BFA5"/>
                        <stop  offset="0.4701" style="stop-color:#00B29A"/>
                        <stop  offset="1" style="stop-color:#009E89"/>
                        </linearGradient>
                        <path style="fill:url(#SVGID_1_);" d="M101.28,124.08c-4.33,0-40.46,0.04-45.29,0.04s-8.11-0.39-9.62-0.67
                        c-7.08-1.29-14.76-2.53-20.62-6.58c-4.48-3.09-9.13-8.83-4.49-18.98c6.47-14.92,12.14-29.64,25.4-31.52
                        c4.44-0.63,10.97-0.56,18.3-0.56s12.16,1.21,15.22,1.88c21.85,4.79,22.77,21.98,24.77,40.68
                        C104.94,108.37,106.65,124.08,101.28,124.08z"/>
                         * */
                        styles[id] = LinearGradient(this, child)
                        // used by fill:url(#id)
                    }
                }
                "radialgradient" -> {
                    val id = child["id"]
                    if (id != null) {
                        styles[id] = RadialGradient(this, child)
                    }
                }
                "style" -> {
                    val id = child["id"]
                    when (val type = child["type"]?.lowercase()) {
                        "text/css" -> {
                            val content = child.children.filterIsInstance<String>().joinToString("\n")
                            CSSReader.read(this, content)
                        }
                        null, "" -> {
                            if (id != null) styles[id] = SVGStyle(this, child)
                        }
                        else -> {
                            LOGGER.warn("Unknown style type $type")
                        }
                    }
                }
                "metadata" -> {
                } // I don't think, that I care...
                else -> {
                    // idc
                    LOGGER.warn("Unknown svg element ${child.type}")
                }
            }
        }
    }

    var buffer: StaticBuffer? = null

    fun createMesh(x0: Float, y0: Float, w: Float, h: Float) {
        val cx = x0 + w * 0.5f
        val cy = y0 + h * 0.5f
        val scale = 2f / h
        val totalPointCount = curves.sumOf { it.triangles.size }
        if (totalPointCount > 0) {
            val buffer = StaticBuffer("SVG", attr, totalPointCount)
            this.buffer = buffer
            val formula = Formula()
            val c0 = Vector4f()
            val c1 = Vector4f()
            val c2 = Vector4f()
            val c3 = Vector4f()
            val stops = Vector4f()
            for (curve in curves) {
                if (curve.triangles.isEmpty()) continue
                val minX = curve.triangles.minOf { it.x }
                val maxX = curve.triangles.maxOf { it.x }
                val minY = curve.triangles.minOf { it.y }
                val maxY = curve.triangles.maxOf { it.y }
                val scaleX = 1f / max(1e-7f, maxX - minX)
                val scaleY = 1f / max(1e-7f, maxY - minY)
                // upload all shapes
                val gradient = curve.gradient
                gradient.fill(formula, c0, c1, c2, c3, stops)
                // if (gradient.colors.size > 1) LOGGER.info("$gradient -> $formula")
                val padding = gradient.spreadMethod.id.toFloat()
                val z = curve.depth
                val circle = if (formula.isCircle) 1f else 0f
                for (v in curve.triangles) {
                    val vx = v.x
                    val vy = v.y
                    // position, v3
                    val x = ((vx - cx) * scale)
                    val y = ((vy - cy) * scale)
                    buffer.put(x, y, z)
                    // local pos 2
                    buffer.put(((vx - minX) * scaleX), ((vy - minY) * scaleY))
                    // formula 0
                    buffer.put(formula.position)
                    // formula 1
                    buffer.put(formula.directionOrRadius)
                    buffer.put(circle)
                    // color 0-3, v4 each
                    buffer.put(c0)
                    buffer.put(c1)
                    buffer.put(c2)
                    buffer.put(c3)
                    // stops, v4
                    buffer.put(stops)
                    // padding, v1
                    buffer.put(padding)
                }
            }
        }
    }

    var mesh: Mesh? = null

    fun createMesh2(x0: Float, y0: Float, w: Float, h: Float) {
        val cx = x0 + w * 0.5f
        val cy = y0 + h * 0.5f
        val scale = 2f / h
        val totalPointCount = curves.sumOf { it.triangles.size }
        if (totalPointCount > 0) {
            val mesh = Mesh()
            this.mesh = mesh
            val positions = FloatArrayList(totalPointCount * 3)
            val colors = IntArrayList(totalPointCount)
            for (curve in curves) {
                val triangles = curve.triangles
                if (triangles.isEmpty()) continue
                val minX = triangles.minOf { it.x }
                val maxX = triangles.maxOf { it.x }
                val minY = triangles.minOf { it.y }
                val maxY = triangles.maxOf { it.y }
                val scaleX = 1f / max(1e-7f, maxX - minX)
                val scaleY = 1f / max(1e-7f, maxY - minY)
                // upload all shapes
                val gradient = curve.gradient
                val z = curve.depth
                if (gradient.colors.size >= 2) {
                    gradient.sort()

                    fun add(a: Vector2f) {
                        val x = ((a.x - cx) * scale)
                        val y = ((a.y - cy) * scale)
                        positions.add(x, -y, z)
                        val lx = (a.x - minX) * scaleX
                        val ly = (a.y - minY) * scaleY
                        val p = gradient.getProgress(lx, ly)
                        val c = gradient.getColor(p)
                        colors.add(c)
                    }

                    fun tri(a: Vector2f, b: Vector2f, c: Vector2f) {
                        add(a)
                        add(c)
                        add(b)
                    }

                    // extra precision for circles
                    val m = if (gradient is RadialGradient) 10f else 3f

                    /**
                     * supposed to split a triangle for the gradient
                     * not working!!!
                     * */
                    fun triX(a: Vector2f, b: Vector2f, c: Vector2f) {
                        val lxa = (a.x - minX) * scaleX
                        val lya = (a.y - minY) * scaleY
                        val lxb = (b.x - minX) * scaleX
                        val lyb = (b.y - minY) * scaleY
                        val lxc = (c.x - minX) * scaleX
                        val lyc = (c.y - minY) * scaleY
                        val idx0 = gradient.getIndex(gradient.getProgress(lxa, lya)) * m
                        val idx1 = gradient.getIndex(gradient.getProgress(lxb, lyb)) * m
                        val idx2 = gradient.getIndex(gradient.getProgress(lxc, lyc)) * m
                        val i0 = floor(idx0)
                        val i1 = floor(idx1)
                        val i2 = floor(idx2)
                        if (i0 == i1 && i1 == i2) {
                            tri(a, b, c)
                        } else {

                            fun tri(a: Vector2f, b: Vector2f, c: Vector2f, swap: Boolean) {
                                if (swap) {
                                    tri(b, a, c)
                                } else {
                                    tri(a, b, c)
                                }
                            }

                            fun quad(a: Vector2f, b: Vector2f, c: Vector2f, d: Vector2f, swap: Boolean) {
                                if (swap) {
                                    tri(b, a, c)
                                    tri(a, d, c)
                                } else {
                                    tri(a, b, c)
                                    tri(a, c, d)
                                }
                            }

                            fun sub2(
                                a: Vector2f, b: Vector2f, c: Vector2f,
                                ai: Float, bi: Float, ci: Float,
                                swap: Boolean
                            ) {
                                // subdivide the triangle
                                // val aj = floor(ai)
                                // val bj = floor(bi)
                                // val cj = floor(ci)
                                // (cj in aj..bj)
                                var x = ai
                                while (true) {
                                    val nx = if (x < ci) {
                                        // left side
                                        min(min(x + 1f, ci), floor(x) + 1f)
                                    } else if (x == ci) {
                                        // after middle
                                        min(bi, floor(ci + 1f))
                                    } else {
                                        min(bi, x + 1f)
                                    }
                                    if (nx > bi) {
                                        throw IllegalStateException()
                                    }
                                    // add tripe from x to nx
                                    if (x == ai) {
                                        // add left triangle
                                        val ab = Vector2f(a).mix(b, unmix(ai, bi, nx))
                                        val ac = Vector2f(a).mix(c, unmix(ai, ci, nx))
                                        tri(a, ab, ac, swap)
                                    } else if (nx == bi) {
                                        // add right triangle
                                        val ab = Vector2f(a).mix(b, unmix(ai, bi, x))
                                        val bc = Vector2f(b).mix(c, unmix(bi, ci, x))
                                        tri(ab, b, bc, swap)
                                    } else if (nx <= ci) {
                                        val r0 = Vector2f(c)
                                        val r1 = Vector2f(c)
                                        if (nx <= ci) {
                                            // add quad on left side
                                            r0.mix(a, unmix(ci, ai, x))
                                            r1.mix(a, unmix(ci, ai, nx))
                                        } else {
                                            // add quad on left side
                                            r0.mix(b, unmix(ci, bi, x))
                                            r1.mix(b, unmix(ci, bi, nx))
                                        }
                                        // add quad
                                        val q0 = Vector2f(a).mix(b, unmix(ai, bi, x))
                                        val q1 = Vector2f(a).mix(b, unmix(ai, bi, nx))
                                        quad(q0, q1, r1, r0, swap)
                                    }
                                    if (nx == bi) break
                                    x = nx
                                }
                                // tri(a, b, c, swap)
                            }

                            fun sub3(
                                a: Vector2f, b: Vector2f, c: Vector2f,
                                ai: Float, bi: Float, ci: Float, swap: Boolean
                            ) {
                                if (ci < bi) sub2(a, b, c, ai, bi, ci, swap)
                                else sub2(a, b, c, ai, ci, bi, !swap)
                            }

                            fun sub(a: Vector2f, b: Vector2f, c: Vector2f, ai: Float, bi: Float, ci: Float) {
                                if (ai < bi) sub3(a, b, c, ai, bi, ci, false)
                                else sub3(b, a, c, bi, ai, ci, true)
                            }

                            val ab = abs(i1 - i0)
                            val bc = abs(i2 - i1)
                            val ca = abs(i0 - i2)
                            when {
                                ab >= bc && ab >= ca -> {
                                    // ab is primary
                                    sub(a, b, c, idx0, idx1, idx2)
                                }
                                bc >= ca -> {
                                    // bc is primary
                                    sub(b, c, a, idx1, idx2, idx0)
                                }
                                else -> {
                                    // ca is primary
                                    sub(c, a, b, idx2, idx0, idx1)
                                }
                            }
                        }
                    }

                    for (i in triangles.indices step 3) {
                        val a = triangles[i]
                        val b = triangles[i + 1]
                        val c = triangles[i + 2]
                        val lxa = (a.x - minX) * scaleX
                        val lya = (a.y - minY) * scaleY
                        val lxb = (b.x - minX) * scaleX
                        val lyb = (b.y - minY) * scaleY
                        val lxc = (c.x - minX) * scaleX
                        val lyc = (c.y - minY) * scaleY
                        // if is circle: check if the point is within this triangle, and if so, split there
                        // todo get triX working
                        if (gradient is RadialGradient) {
                            val p = gradient.position
                            if (pointInTriangle(lxa, lya, lxb, lyb, lxc, lyc, p.x, p.y) ||
                                pointInTriangle(lxa, lya, lxc, lyc, lxb, lyb, p.x, p.y)
                            ) {
                                val p2 = Vector2f(p).div(scaleX, scaleY).add(minX, minY)
                                tri(a, b, p2)
                                tri(b, c, p2)
                                tri(c, a, p2)
                                continue
                            }
                        }
                        tri(a, b, c)
                    }
                } else {// no gradient -> fast path
                    val color = gradient.getColor(0f)
                    for (vi in triangles.indices.reversed()) {
                        val v = triangles[vi]
                        val x = ((v.x - cx) * scale)
                        val y = ((v.y - cy) * scale)
                        positions.add(x, -y, z)
                        colors.add(color)
                    }
                }
            }
            mesh.positions = positions.toFloatArray()
            mesh.color0 = colors.toIntArray()
        }
    }

    fun convertStyle(xml: XMLNode) {
        val style = xml["style"]
        if (style != null) {
            val properties = style.split(';')
            SimpleYAMLReader.read(properties.iterator(), false, xml.attributes)
        }
        val id = xml["id"]
        if (id != null) {
            val style2 = ids[id]
            if (style2 != null) {
                for ((key, value) in style2) {
                    if (key !in xml) {
                        xml[key] = value
                    }
                }
            }
        }
    }

    var currentCurve = ArrayList<Vector2f>(128)
    val curves = ArrayList<SVGCurve>()

    var x = 0f
    var y = 0f

    var reflectedX = 0f
    var reflectedY = 0f

    lateinit var currentStyle: SVGStyle
    var currentFill = false

    fun init(style: SVGStyle, fill: Boolean) {
        end(false)
        currentStyle = style
        currentFill = fill
        // each new element is relative to its parent
        x = 0f
        y = 0f
    }

    fun endElement() {
        end(false)
        z += deltaZ
    }

    fun addLine(xml: XMLNode, style: SVGStyle, fill: Boolean) {
        init(style, fill)
        moveTo(xml["x1"]!!.toFloat(), xml["y1"]!!.toFloat())
        lineTo(xml["x2"]!!.toFloat(), xml["y2"]!!.toFloat())
        endElement()
    }

    fun addPath(xml: XMLNode, style: SVGStyle, fill: Boolean) {
        init(style, fill)
        val data = xml["d"] ?: return

        readSVGPath(
            data,
            ::close,
            { s, v ->
                when (s) {
                    'H' -> lineTo(v, y)
                    'h' -> lineTo(x + v, y)
                    'V' -> lineTo(x, v)
                    'v' -> lineTo(x, y + v)
                }
            },
            { s, x0, y0 ->
                when (s) {
                    'M' -> moveTo(x0, y0)
                    'm' -> moveTo(x + x0, y + y0)
                    'L' -> lineTo(x0, y0)
                    'l' -> lineTo(x + x0, y + y0)
                    'T' -> quadraticTo(reflectedX, reflectedY, x0, y0)
                    't' -> quadraticTo(reflectedX, reflectedY, x + x0, y + y0)
                }
            },
            { s, x0, y0, x1, y1 ->
                when (s) {
                    'S' -> cubicTo(reflectedX, reflectedY, x0, y0, x1, y1)
                    's' -> cubicTo(reflectedX, reflectedY, x + x0, y + y0, x + x1, y + y1)
                    'Q' -> quadraticTo(x0, y0, x1, y1)
                    'q' -> quadraticTo(x + x0, y + y0, x + x1, y + y1)
                }
            },
            { s, x0, y0, x1, y1, x2, y2 ->
                when (s) {
                    'C' -> cubicTo(x0, y0, x1, y1, x2, y2)
                    'c' -> cubicTo(x + x0, y + y0, x + x1, y + y1, x + x2, y + y2)
                }
            },
            { s, rx, ry, rot, la, sw, x2, y2 ->
                when (s) {
                    'A' -> arcTo(rx, ry, rot, la, sw, x2, y2)
                    'a' -> arcTo(rx, ry, rot, la, sw, x + x2, y + y2)
                }
            })

        endElement()
    }

    // http://xahlee.info/REC-SVG11-20110816/implnote.html#ArcImplementationNotes
    fun arcTo(
        rx: Float, ry: Float, xAxisRotation: Float,
        largeArcFlag: Boolean, sweepFlag: Boolean,
        x2: Float, y2: Float
    ) {

        if (rx == 0f && ry == 0f) return lineTo(x2, y2)

        if (rx < 0f || ry < 0f) return arcTo(abs(rx), abs(ry), xAxisRotation, largeArcFlag, sweepFlag, x2, y2)

        val x1 = this.x
        val y1 = this.y

        val angle = xAxisRotation.toRadians()
        val cos = cos(angle)
        val sin = sin(angle)

        val localX = (x1 - x2) / 2f
        val localY = (y1 - y2) / 2f

        val x12 = cos * localX + sin * localY
        val y12 = -sin * localX + cos * localY

        val scaleCorrection = length(x12 / rx, y12 / ry)
        if (scaleCorrection > 1f) {
            return arcTo(rx * scaleCorrection, ry * scaleCorrection, xAxisRotation, largeArcFlag, sweepFlag, x2, y2)
        }

        val sign = if (largeArcFlag != sweepFlag) 1f else -1f
        val tx = rx * rx * y12 * y12
        val ty = ry * ry * x12 * x12
        val c2Length = sign * sqrt((rx * rx * ry * ry - (tx + ty)) / (tx + ty))
        val cx2 = c2Length * rx * y12 / ry
        val cy2 = c2Length * -ry * x12 / rx

        val avgX = (x1 + x2) / 2f
        val avgY = (y1 + y2) / 2f
        val cx = cos * cx2 - sin * cy2 + avgX
        val cy = sin * cx2 + cos * cy2 + avgY

        val qx = (x12 - cx2) / rx
        val qy = (y12 - cy2) / ry

        val twoPi = TAUf

        val theta0 = angle(1f, 0f, qx, qy)
        var deltaTheta = angle(qx, qy, -(x12 + cx2) / rx, -(y12 + cy2) / ry)// % twoPi

        if (sweepFlag) {
            if (deltaTheta <= 0f) deltaTheta += twoPi
        } else {
            if (deltaTheta >= 0f) deltaTheta -= twoPi
        }

        val angleDegrees = deltaTheta * 180 / PI

        val steps = max(3, (abs(angleDegrees) * stepsPerDegree).roundToIntOr())
        for (i in 1 until steps) {
            val theta = theta0 + deltaTheta * i / steps
            val localX2 = rx * cos(theta)
            val localY2 = ry * sin(theta)
            val rotX = cos * localX2 - sin * localY2
            val rotY = sin * localX2 + cos * localY2
            lineTo(cx + rotX, cy + rotY)
        }

        lineTo(x2, y2)
    }

    fun angle(ux: Float, uy: Float, vx: Float, vy: Float): Float {
        val sign = if (ux * vy - uy * vx > 0f) 1f else -1f
        val dotTerm = (ux * vx + uy * vy) / sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        return sign * acos(clamp(dotTerm, -1f, 1f))
    }

    fun addPolylineBody(xml: XMLNode) {
        val data = xml["points"]!!

        var i = 0
        fun read(): Float {
            var j = i
            spaces@ while (true) {
                when (data[j]) {
                    ' ', '\t', '\r', '\n', ',' -> j++
                    else -> break@spaces
                }
            }
            i = j
            when (data[j]) {
                '+', '-' -> j++
            }
            when (data[j]) {
                '.' -> {
                    // LOGGER.info("starts with .")
                    j++
                    int@ while (true) {
                        when (data.getOrNull(j)) {
                            in '0'..'9' -> j++
                            else -> break@int
                        }
                    }
                }
                else -> {
                    int@ while (true) {
                        when (data.getOrNull(j)) {
                            in '0'..'9' -> j++
                            else -> break@int
                        }
                    }
                    if (data.getOrNull(j) == '.') {
                        j++
                        int@ while (true) {
                            when (data.getOrNull(j)) {
                                in '0'..'9' -> j++
                                else -> break@int
                            }
                        }
                    }
                }
            }

            when (data.getOrNull(j)) {
                'e', 'E' -> {
                    j++
                    when (data.getOrNull(j)) {
                        '+', '-' -> j++
                    }
                    int@ while (true) {
                        when (data.getOrNull(j)) {
                            in '0'..'9' -> j++
                            else -> break@int
                        }
                    }
                }
            }
            val value = data.substring(i, j).toFloat()
            i = j
            return value
        }

        while (i < data.length) {
            when (data[i]) {
                ' ', '\t', '\r', '\n' -> i++
                else -> {
                    val x = read()
                    val y = read()
                    lineTo(x, y)
                }
            }
        }
    }

    fun addPolyline(xml: XMLNode, style: SVGStyle, fill: Boolean) {
        init(style, fill)
        addPolylineBody(xml)
        endElement()
    }

    fun addPolygon(xml: XMLNode, style: SVGStyle, fill: Boolean) {
        init(style, fill)
        addPolylineBody(xml)
        close()
        endElement()
    }

    fun addEllipse(xml: XMLNode, style: SVGStyle, fill: Boolean) {
        init(style, fill)
        addSimpleEllipse(xml["cx"]!!.toFloat(), xml["cy"]!!.toFloat(), xml["rx"]!!.toFloat(), xml["ry"]!!.toFloat())
        endElement()
    }

    fun addSimpleEllipse(cx: Float, cy: Float, rx: Float, ry: Float) {
        val steps = max(7, (360 * stepsPerDegree).roundToIntOr())
        moveTo(cx + rx, cy)
        for (i in 1 until steps) {
            val f = TAUf * i / steps
            val s = sin(f)
            val c = cos(f)
            lineTo(cx + c * rx, cy + s * ry)
        }
        close()
    }

    fun addRectangle(xml: XMLNode, style: SVGStyle, fill: Boolean) {

        init(style, fill)

        val rx = max(xml["rx"]?.toFloat() ?: 0f, 0f)
        val ry = max(xml["ry"]?.toFloat() ?: 0f, 0f)

        val x = xml["x"]?.toFloat() ?: 0f
        val y = xml["y"]?.toFloat() ?: 0f
        val w = xml["width"]!!.toFloat()
        val h = xml["height"]!!.toFloat()

        val curveSteps = max(rx, ry).roundToIntOr(2)

        if (rx > 0f || ry > 0f) {

            // top line
            moveTo(x + rx, y)
            lineTo(x + w - rx, y)
            // curve down
            for (i in 1 until curveSteps) {
                addCirclePoint(x + w - rx, y + ry, rx, ry, i, 3, curveSteps)
            }
            // right line
            lineTo(x + w, y + ry)
            lineTo(x + w, y + h - ry)
            // curve from right to bottom
            for (i in 1 until curveSteps) {
                addCirclePoint(x + w - rx, y + h - ry, rx, ry, i, 0, curveSteps)
            }
            // bottom line
            lineTo(x + w - rx, y + h)
            lineTo(x + rx, y + h)
            // curve from bottom to left
            for (i in 1 until curveSteps) {
                addCirclePoint(x + rx, y + h - ry, rx, ry, i, 1, curveSteps)
            }
            // left line
            lineTo(x, y + h - ry)
            lineTo(x, y + ry)
            for (i in 1 until curveSteps) {
                addCirclePoint(x + rx, y + ry, rx, ry, i, 2, curveSteps)
            }
        } else {
            moveTo(x, y)
            lineTo(x + w, y)
            lineTo(x + w, y + h)
            lineTo(x, y + h)
        }

        close()
        endElement()
    }

    fun addCirclePoint(x: Float, y: Float, rx: Float, ry: Float, i: Int, q: Int, steps: Int) {
        val angle = (i + q * steps) * 0.5f * PIf / steps
        lineTo(x + rx * cos(angle), y + ry * sin(angle))
    }

    fun addCircle(xml: XMLNode, style: SVGStyle, fill: Boolean) {
        init(style, fill)
        val r = xml["r"]!!.toFloat()
        val cx = xml["cx"]!!.toFloat()
        val cy = xml["cy"]!!.toFloat()
        addSimpleEllipse(cx, cy, r, r)
        endElement()
    }

    fun angleDegrees(dx1: Float, dy1: Float, dx2: Float, dy2: Float): Float {
        val div = (dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
        if (div == 0f) return 57.29578f
        return 57.29578f * (acos(clamp((dx1 * dx2 + dy1 * dy2) / sqrt(div), -1f, 1f)))
    }

    fun steps(dx1: Float, dy1: Float, dx2: Float, dy2: Float) =
        max((angleDegrees(dx1, dy1, dx2, dy2) * stepsPerDegree).roundToIntOr(2), 2)

    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) {

        val steps = steps(x1 - this.x, y1 - this.y, x - x2, y - y2)
        for (i in 1 until steps) {
            val f = i * 1f / steps
            val g = 1f - f
            val a = g * g * g
            val b = 3 * g * g * f
            val c = 3 * g * f * f
            val d = f * f * f
            currentCurve += Vector2f(
                this.x * a + x1 * b + x2 * c + x * d,
                this.y * a + y1 * b + y2 * c + y * d
            )
        }

        reflectedX = 2 * x - x2
        reflectedY = 2 * y - y2

        lineTo(x, y)
    }

    fun quadraticTo(x1: Float, y1: Float, x: Float, y: Float) {

        val steps = steps(x1 - this.x, y1 - this.y, x - x1, y - y1)
        for (i in 1 until steps) {
            val f = i * 1f / steps
            val g = 1f - f
            val a = g * g
            val b = 2 * g * f
            val c = f * f
            currentCurve += Vector2f(
                this.x * a + x1 * b + x * c,
                this.y * a + y1 * b + y * c
            )
        }

        reflectedX = 2 * x - x1
        reflectedY = 2 * y - y1

        lineTo(x, y)
    }

    fun lineTo(x: Float, y: Float) {

        currentCurve += Vector2f(x, y)

        this.x = x
        this.y = y
    }

    fun moveTo(x: Float, y: Float) {

        end(false)

        currentCurve += Vector2f(x, y)

        this.x = x
        this.y = y
    }

    fun end(closed: Boolean) {

        if (currentCurve.isNotEmpty()) {
            val transform = transform
            for (it in currentCurve) {
                val x = it.x
                val y = it.y
                it.set(
                    transform.m00 * x + transform.m10 * y + transform.m30,
                    transform.m01 * x + transform.m11 * y + transform.m31,
                )
            }
            curves += SVGCurve(
                currentCurve,
                closed, z,
                if (currentFill) currentStyle.fill!! else currentStyle.stroke!!,
                if (currentFill) 0f else currentStyle.strokeWidth
            )
            currentCurve = ArrayList()
        }
    }

    fun close() = end(true)

    companion object {

        private val LOGGER = LogManager.getLogger(SVGMesh::class)

        val attr = listOf(
            Attribute("aLocalPosition", 3),
            Attribute("aLocalPos2", 2),
            Attribute("aFormula0", 4),
            Attribute("aFormula1", 1),
            Attribute("aColor0", 4),
            Attribute("aColor1", 4),
            Attribute("aColor2", 4),
            Attribute("aColor3", 4),
            Attribute("aStops", 4),
            Attribute("aPadding", 1)
        )

        fun readAsFolder(file: FileReference, callback: Callback<InnerFolder>) {
            // Engine.requestShutdown()
            file.inputStream { str, exc ->
                if (str != null) {
                    val svg = SVGMesh()
                    svg.parse(XMLReader().read(str.reader()) as XMLNode)
                    val mesh = svg.mesh // may be null if the parsing failed / the svg is blank
                    if (mesh != null) {
                        val folder = InnerFolder(file)
                        val prefab = Prefab("Mesh")
                        prefab["positions"] = mesh.positions
                        prefab["color0"] = mesh.color0
                        prefab._sampleInstance = mesh
                        folder.createPrefabChild("Scene.json", prefab)
                        // todo create Images folder, where the svg is interpreted as an image :)
                        callback.ok(folder)
                    } else callback.err(IOException("No contents could be parsed"))
                } else callback.err(exc)
            }
        }
    }
}