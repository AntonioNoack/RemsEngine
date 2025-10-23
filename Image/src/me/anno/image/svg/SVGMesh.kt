package me.anno.image.svg

import me.anno.config.DefaultConfig
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.prefab.Prefab
import me.anno.fonts.signeddistfields.Contour
import me.anno.fonts.signeddistfields.edges.CubicSegment
import me.anno.fonts.signeddistfields.edges.EdgeSegment
import me.anno.fonts.signeddistfields.edges.LinearSegment
import me.anno.fonts.signeddistfields.edges.QuadraticSegment
import me.anno.image.svg.SVGToBuffer.createBuffer
import me.anno.image.svg.SVGToMesh.createMesh
import me.anno.image.svg.SVGTransform.applyTransform
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
import me.anno.utils.async.Callback
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.Floats.roundToIntOr
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix3x2f
import org.joml.Matrix4dArrayList
import org.joml.Vector2f
import java.io.IOException
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// to do animated svg
// to do gradients
// to do don't use depth, use booleans on triangles to remove flickering

class SVGMesh(xml: XMLNode) {

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
                    if (transform2 is String) {
                        transform.pushMatrix()
                        applyTransform(transform, transform2)
                    }
                    parseChildren(child.children, child)
                    if (transform2 is String) {
                        transform.popMatrix()
                    }
                }
                "switch", "foreignobject", "i:pgfref", "i:pgf", "defs" -> {
                    parseChildren(child.children, parentGroup)
                }
                "lineargradient" -> {
                    val id = child["id"]
                    if (id is String) {
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
                    if (id is String) {
                        styles[id] = RadialGradient(this, child)
                    }
                }
                "style" -> {
                    val id = child["id"]
                    when (val type = (child["type"])?.lowercase()) {
                        "text/css" -> {
                            val content = child.children.filterIsInstance<String>().joinToString("\n")
                            CSSReader.read(this, content)
                        }
                        null, "" -> if (id is String) styles[id] = SVGStyle(this, child)
                        else -> LOGGER.warn("Unknown style type $type")
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

    val totalPointCount get() = curves.sumOf { it.trianglesIndices.size }
    val isValid get() = totalPointCount > 0

    fun convertStyle(xml: XMLNode) {
        val style = xml["style"]
        if (style != null) {
            val properties = style.split(';')
            @Suppress("UNCHECKED_CAST")
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

    val currentCurve = ArrayList<Vector2f>(128)
    val curves = ArrayList<SVGCurve>()

    val currentContour = ArrayList<EdgeSegment>()
    val contours = ArrayList<Contour>()

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
        moveTo(getFloat(xml["x1"]), getFloat(xml["y1"]))
        lineTo(getFloat(xml["x2"]), getFloat(xml["y2"]))
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
        val data = xml["points"] as String

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
        addSimpleEllipse(
            getFloat(xml["cx"]), getFloat(xml["cy"]),
            getFloat(xml["rx"]), getFloat(xml["ry"])
        )
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

        val rx = max(getFloat(xml["rx"]), 0f)
        val ry = max(getFloat(xml["ry"]), 0f)

        val x = getFloat(xml["x"])
        val y = getFloat(xml["y"])
        val w = getFloat(xml["width"])
        val h = getFloat(xml["height"])

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
        val r = getFloat(xml["r"])
        val cx = getFloat(xml["cx"])
        val cy = getFloat(xml["cy"])
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

        val prev = currentCurve.lastOrNull()

        val steps = steps(x1 - this.x, y1 - this.y, x - x2, y - y2)
        for (i in 1 until steps) {
            val f = i * 1f / steps
            val g = 1f - f
            val a = g * g * g
            val b = 3 * g * g * f
            val c = 3 * g * f * f
            val d = f * f * f
            currentCurve += transformed(
                this.x * a + x1 * b + x2 * c + x * d,
                this.y * a + y1 * b + y2 * c + y * d
            )
        }

        reflectedX = 2 * x - x2
        reflectedY = 2 * y - y2

        lineTo(x, y, false)

        val curr = currentCurve.last()
        currentContour += CubicSegment(
            prev ?: transformed(x, y),
            transformed(x1, y1),
            transformed(x2, y2),
            curr
        )
    }

    fun quadraticTo(x1: Float, y1: Float, x: Float, y: Float) {

        val prev = currentCurve.lastOrNull()
        val px = this.x
        val py = this.y

        val steps = steps(x1 - this.x, y1 - this.y, x - x1, y - y1)
        for (i in 1 until steps) {
            val f = i * 1f / steps
            val g = 1f - f
            val a = g * g
            val b = 2 * g * f
            val c = f * f
            currentCurve += transformed(
                this.x * a + x1 * b + x * c,
                this.y * a + y1 * b + y * c
            )
        }

        reflectedX = 2 * x - x1
        reflectedY = 2 * y - y1

        lineTo(x, y, false)

        val last = currentCurve.last()
        currentContour += QuadraticSegment(
            prev ?: transformed(px, py),
            transformed(x1, y1),
            last
        )
    }

    fun lineTo(x: Float, y: Float, addLinearSegment: Boolean = true) {

        val curr = transformed(x, y)
        val prev = currentCurve.lastOrNull()
        if (prev != null && addLinearSegment) {
            currentContour.add(LinearSegment(prev, curr))
        }

        currentCurve += curr

        this.x = x
        this.y = y
    }

    fun moveTo(x: Float, y: Float) {

        end(false)

        currentCurve += transformed(x, y)

        this.x = x
        this.y = y
    }

    private fun transformX(vx: Float, vy: Float): Float =
        (transform.m00 * vx + transform.m10 * vy + transform.m30).toFloat()

    private fun transformY(vx: Float, vy: Float): Float =
        (transform.m01 * vx + transform.m11 * vy + transform.m31).toFloat()

    private fun transformed(x: Float, y: Float) = Vector2f(
        transformX(x, y),
        transformY(x, y)
    )


    fun end(closed: Boolean) {
        if (currentCurve.isEmpty()) return

        val gradient = if (currentFill) currentStyle.fill!! else currentStyle.stroke!!
        val width = if (currentFill) 0f else currentStyle.strokeWidth
        curves += SVGCurve(currentCurve, closed, z, gradient, width)
        currentCurve.clear()

        if (currentContour.isNotEmpty()) {
            if (currentFill) {
                val color = gradient.getColor(0.5f) // best we can do
                val segments = ArrayList(currentContour)
                contours.add(Contour(segments, z, color))
            } // todo else create 2d contour from 1d contour
            currentContour.clear()
        }
    }

    fun close() = end(true)

    fun getTransformedContours(
        cx: Float, cy: Float,
        height: Float,
        generateCCW: Boolean,
    ): List<Contour> {
        val scale = height / this.h0
        val transform = Matrix3x2f(
            scale, 0f,
            0f, scale,
            cx - x0 * scale,
            cy - y0 * scale
        )
        return contours.map { contour ->
            val flipped = generateCCW == contour.isCCW()
            val transformed = contour.segments.map { segment -> segment.transformed(transform, flipped) }
            val segments = if (flipped) transformed else transformed.asReversed()
            Contour(segments, contour.z, contour.color)
        }
    }

    val x0: Float
    val y0: Float
    val w0: Float
    val h0: Float

    init {

        parseChildren(xml.children, null)

        val viewBox = (xml["viewBox"] ?: "0 0 100 100")
            .replace(',', ' ')
            .split(' ')
            .filter { !it.isBlank2() }
            .map { it.toFloat() }

        x0 = viewBox[0]
        y0 = viewBox[1]

        w0 = viewBox[2]
        h0 = viewBox[3]

        bounds.minX = -w0 / (2f * h0)
        bounds.maxX = +w0 / (2f * h0)
        bounds.minY = -0.5f
        bounds.maxY = +0.5f
    }

    val mesh = lazy { createMesh() }
    val buffer = lazy { createBuffer() }

    companion object {

        private val LOGGER = LogManager.getLogger(SVGMesh::class)

        fun readAsFolder(file: FileReference, callback: Callback<InnerFolder>) {
            file.inputStream { str, exc ->
                if (str != null) {
                    val svg = SVGMesh(XMLReader(str.reader()).readXMLNode()!!)
                    val mesh = svg.mesh.value // may be null if the parsing failed / the svg is blank
                    if (mesh != null) {
                        val folder = InnerFolder(file)
                        val prefab = Prefab("Mesh")
                        prefab["positions"] = mesh.positions
                        prefab["colors0"] = mesh.color0
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