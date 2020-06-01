package me.anno.objects.meshes.svg

import me.anno.config.DefaultConfig
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.io.xml.XMLElement
import me.anno.utils.clamp
import org.joml.Vector2f
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import java.lang.RuntimeException
import javax.imageio.ImageIO
import kotlin.math.*

// todo animated svg
// todo transforms
// todo gradients
// todo don't use depth, use booleans on triangles to remove flickering

class SVGMesh {

    // read svg
    // creates mesh with colors

    val stepsPerDegree = DefaultConfig["format.svg.stepsPerDegree", 0.1f]

    var z = 0f
    val deltaZ = 0.001f

    fun parse(svg: XMLElement){
        parseChildren(svg.children, null)
        val viewBox = (svg["viewBox"] ?: "0 0 100 100").split(' ').map { it.toFloat() }
        createMesh(viewBox[0], viewBox[1], viewBox[2], viewBox[3])
    }

    fun parseChildren(children: List<Any>, parentGroup: XMLElement?){
        children.forEach {
            (it as? XMLElement)?.apply {
                convertStyle(this)
                parentGroup?.properties?.forEach { key, value ->
                    // todo apply transforms differently
                    if(key !in this.properties){
                        this[key] = value
                    }
                }
                val style = SVGStyle(this)
                when(type.toLowerCase()){
                    "circle" -> {
                        if(style.isFill) addCircle(this, style, true)
                        if(style.isStroke) addCircle(this, style, false)
                    }
                    "rect" -> {
                        if(style.isFill) addRectangle(this, style, true)
                        if(style.isStroke) addRectangle(this, style, false)
                    }
                    "ellipse" -> {
                        if(style.isFill) addEllipse(this, style, true)
                        if(style.isStroke) addEllipse(this, style, false)
                    }
                    "line" -> {
                        if(style.isFill) addLine(this, style, true)
                    }
                    "polyline" -> {
                        if(style.isFill) addPolyline(this, style, true)
                        if(style.isStroke) addPolyline(this, style, false)
                    }
                    "polygon" -> {
                        if(style.isFill) addPolygon(this, style, true)
                        if(style.isStroke) addPolygon(this, style, false)
                    }
                    "path" -> {
                        if(style.isFill) addPath(this, style, true)
                        if(style.isStroke) addPath(this, style, false)
                    }
                    "g" -> {
                        parseChildren(this.children, this)
                    }
                    "switch", "foreignobject", "i:pgfref", "i:pgf" -> {
                        parseChildren(this.children, parentGroup)
                    }
                    else -> throw RuntimeException("Unknown svg element $type")
                }
            }
        }
    }

    var buffer: StaticFloatBuffer? = null

    fun debugMesh(x: Float, y: Float, w: Float, h: Float){
        val x0 = x+w/2
        val y0 = y+h/2
        val debugImageSize = 1000
        val scale = debugImageSize/h
        val img = BufferedImage(debugImageSize, debugImageSize, 1)
        val gfx = img.graphics as Graphics2D
        fun ix(v: Vector2f) = debugImageSize/2 + ((v.x-x0)*scale).roundToInt()
        fun iy(v: Vector2f) = debugImageSize/2 + ((v.y-y0)*scale).roundToInt()
        curves.forEach {
            val color = it.color or 0x333333
            val triangles = it.triangles
            gfx.color = Color(color, false)
            for(i in triangles.indices step 3){
                val a = triangles[i]
                val b = triangles[i+1]
                val c = triangles[i+2]
                gfx.drawLine(ix(a), iy(a), ix(b), iy(b))
                gfx.drawLine(ix(b), iy(b), ix(c), iy(c))
                gfx.drawLine(ix(c), iy(c), ix(a), iy(a))
            }
        }
        ImageIO.write(img, "png", File("C:/Users/Antonio/Desktop/svg/tiger.png"))
    }

    fun createMesh(x0: Float, y0: Float, w: Float, h: Float){
        val scale = 1f/h
        val totalPointCount = curves.sumBy { it.triangles.size }
        val totalFloatCount = totalPointCount * 7 // xyz, rgba
        if(totalPointCount > 0){
            val buffer = StaticFloatBuffer(listOf(
                Attribute("attr0",3), Attribute("attr1", 4)
            ), totalFloatCount)
            this.buffer = buffer
            curves.forEach {
                val color = it.color
                val r = color.shr(16).and(255)/255f
                val g = color.shr(8).and(255)/255f
                val b = color.and(255)/255f
                val a = color.shr(24).and(255)/255f
                it.triangles.forEach { v ->
                    buffer.put((v.x-x0)*scale, (v.y-y0)*scale, it.depth)
                    //buffer.put(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
                    //println(Vector3f((v.x-x0)*scale, (v.y-y0)*scale, it.depth).print())
                    buffer.put(r, g, b, a)
                }
            }
        }
        // println("created buffer $x $y $scale of curves with size $totalPointCount")
    }

    fun convertStyle(xml: XMLElement){
        val style = xml["style"] ?: return
        val properties = style.split(';')
        properties.forEach {
            val index = it.indexOf(':')
            if(index in 1 until it.lastIndex){
                val name = it.substring(0, index).trim()
                val value = it.substring(index+1).trim()
                xml[name] = value
            }
        }
    }

    val currentCurve = ArrayList<Vector2f>(128)
    val curves = ArrayList<SVGCurve>()

    var x = 0f
    var y = 0f

    var reflectedX = 0f
    var reflectedY = 0f

    lateinit var currentStyle: SVGStyle
    var currentFill = false

    fun init(style: SVGStyle, fill: Boolean){
        end(false)
        currentStyle = style
        currentFill = fill
    }

    fun endElement(){
        end(false)
        z += deltaZ
    }

    fun addLine(xml: XMLElement, style: SVGStyle, fill: Boolean){
        init(style, fill)
        moveTo(xml["x1"]!!.toFloat(), xml["y1"]!!.toFloat())
        lineTo(xml["x2"]!!.toFloat(), xml["y2"]!!.toFloat())
        endElement()
    }

    fun addPath(xml: XMLElement, style: SVGStyle, fill: Boolean){
        init(style, fill)
        val data = xml["d"] ?: return
        var i = 0
        fun read(): Float {
            var j = i
            spaces@while(true){
                when(data[j]){
                    ' ', '\t', '\r', '\n', ',' -> j++
                    else -> break@spaces
                }
            }
            i = j
            when(data[j]){
                '+', '-' -> j++
            }
            when(data[j]){
                '.' -> {
                    // println("starts with .")
                    j++
                    int@while(true){
                        when(data[j]){
                            in '0' .. '9' -> j++
                            else -> break@int
                        }
                    }
                }
                else -> {
                    int@while(true){
                        when(data[j]){
                            in '0' .. '9' -> j++
                            else -> break@int
                        }
                    }
                    if(data[j] == '.'){
                        j++
                        int@while(true){
                            when(data[j]){
                                in '0' .. '9' -> j++
                                else -> break@int
                            }
                        }
                    }
                }
            }

            when(data[j]){
                'e', 'E' -> {
                    j++
                    when(data[j]){
                        '+', '-' -> j++
                    }
                    int@while(true){
                        when(data[j]){
                            in '0' .. '9' -> j++
                            else -> break@int
                        }
                    }
                }
            }
            // println("'${data.substring(i, j)}' + ${data.substring(j, j+10)}")
            val value = data.substring(i, j).toFloat()
            i = j
            return value
        }

        var lastAction = ' '
        fun parseAction(symbol: Char): Boolean {
            try {
                when(symbol){
                    ' ', '\t', '\r', '\n' -> return false
                    'M' -> moveTo(read(), read())
                    'm' -> moveTo(x + read(), y + read())
                    'L' -> lineTo(read(), read())
                    'l' -> lineTo(x + read(), y + read())
                    'H' -> lineTo(read(), y)
                    'h' -> lineTo(x + read(), y)
                    'V' -> lineTo(x, read())
                    'v' -> lineTo(x, y + read())
                    'C' -> cubicTo(read(), read(), read(), read(), read(), read())
                    'c' -> cubicTo(x + read(), y + read(), x + read(), y + read(), x + read(), y + read())
                    'S' -> cubicTo(reflectedX, reflectedY, read(), read(), read(), read())
                    's' -> cubicTo(reflectedX, reflectedY, x + read(), y + read(), x + read(), y + read())
                    'Q' -> quadraticTo(read(), read(), read(), read())
                    'q' -> quadraticTo(x + read(), y + read(), x + read(), y + read())
                    'T' -> quadraticTo(reflectedX, reflectedY, read(), read())
                    't' -> quadraticTo(reflectedX, reflectedY, x + read(), y + read())
                    'A' -> arcTo(read(), read(), read(), read(), read(), read(), read())
                    'a' -> arcTo(read(), read(), read(), read(), read(), x + read(), y + read())
                    'Z', 'z' -> close()
                    else -> {
                        i--
                        parseAction(lastAction)
                        return false
                    }
                }
            } catch (e: Exception){
                println(data)
                throw e
            }
            return true
        }

        while(i < data.length){
            when(val symbol = data[i++]){
                ' ', '\t', '\r', '\n' -> {}
                else -> {
                    if(parseAction(symbol)){
                        lastAction = symbol
                    }
                }
            }
        }
        endElement()
    }

    fun arcTo(rx: Float, ry: Float,
              xAxisRotation: Float,
              largeArcFlag: Float, sweepFlag: Float,
              x: Float, y: Float){

        // todo arc to...

        lineTo(x, y)

    }

    fun addPolyline(xml: XMLElement, style: SVGStyle, fill: Boolean){
        init(style, fill)
        val data = xml["points"]!!
        // todo parse the points; same format as path; just only straight lines
        endElement()
    }

    fun addEllipse(xml: XMLElement, style: SVGStyle, fill: Boolean){
        init(style, fill)
        addSimpleEllipse(xml["cx"]!!.toFloat(), xml["cy"]!!.toFloat(), xml["rx"]!!.toFloat(), xml["ry"]!!.toFloat())
        endElement()
    }

    fun addSimpleEllipse(cx: Float, cy: Float, rx: Float, ry: Float){
        val steps = max(7, (360 * stepsPerDegree).roundToInt())
        moveTo(cx + rx, cy)
        for(i in 1 until steps){
            val f = (PI*2*i/steps).toFloat()
            val s = sin(f)
            val c = cos(f)
            lineTo(cx + c * rx, cy + s * ry)
        }
        close()
    }

    fun addRectangle(xml: XMLElement, style: SVGStyle, fill: Boolean){
        init(style, fill)
        val rx = max(xml["rx"]?.toFloatOrNull() ?: 0f, 0f)
        val ry = max(xml["ry"]?.toFloatOrNull() ?: 0f, 0f)
        val x = xml["x"]!!.toFloat()
        val y = xml["y"]!!.toFloat()
        val w = xml["width"]!!.toFloat()
        val h = xml["height"]!!.toFloat()

        if(rx > 0f || ry > 0f){

            moveTo(x+rx, y)
            lineTo(x+w-rx, y)
            // todo curve down
            moveTo(x, y+ry)
            lineTo(x+w, y+h-ry)
            // todo curve
            moveTo(x+w-rx, y+h)
            lineTo(x+rx, y+h)
            // todo curve
            // todo curve once more somewhere...
            close()

        } else {
            moveTo(x, y)
            lineTo(x+w, y)
            lineTo(x+w, y+h)
            lineTo(x, y+h)
            close()
        }

        endElement()
    }

    fun addCircle(xml: XMLElement, style: SVGStyle, fill: Boolean){
        init(style, fill)
        val r = xml["r"]!!.toFloat()
        val cx = xml["cx"]!!.toFloat()
        val cy = xml["cy"]!!.toFloat()
        addSimpleEllipse(cx, cy, r, r)
        endElement()
    }

    fun addPolygon(xml: XMLElement, style: SVGStyle, fill: Boolean){
        init(style, fill)
        endElement()
    }

    fun angleDegrees(dx1: Float, dy1: Float, dx2: Float, dy2: Float): Float {
        val div = (dx1*dx1+dy1*dy1) * (dx2*dx2+dy2*dy2)
        if(div == 0f) return 57f
        return 57.2957795f * (acos(clamp((dx1*dx2 + dy1*dy2) / sqrt(div), -1f, 1f)))
    }

    fun steps(dx1: Float, dy1: Float, dx2: Float, dy2: Float) =
        max((angleDegrees(dx1, dy1, dx2, dy2) * stepsPerDegree).roundToInt(), 2)

    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float){

        val steps = steps(x1 - this.x, y1 - this.y, x - x2, y - y2)
        for(i in 1 until steps){
            val f = i * 1f / steps
            val g = 1f-f
            val a = g*g*g
            val b = 3*g*g*f
            val c = 3*g*f*f
            val d = f*f*f
            currentCurve += Vector2f(
                this.x * a + x1 * b + x2 * c + x * d,
                this.y * a + y1 * b + y2 * c + y * d
            )
        }

        reflectedX = 2 * x - x2
        reflectedY = 2 * y - y2

        lineTo(x, y)

    }

    fun quadraticTo(x1: Float, y1: Float, x: Float, y: Float){

        val steps = steps(x1 - this.x, y1 - this.y, x - x1, y - y1)
        for(i in 1 until steps){
            val f = i * 1f / steps
            val g = 1f-f
            val a = g*g
            val b = 2*g*f
            val c = f*f
            currentCurve += Vector2f(
                this.x * a + x1 * b + x * c,
                this.y * a + y1 * b + y * c
            )
        }

        reflectedX = 2 * x - x1
        reflectedY = 2 * y - y1

        lineTo(x, y)

    }

    fun lineTo(x: Float, y: Float){

        currentCurve += Vector2f(x, y)

        this.x = x
        this.y = y

    }

    fun moveTo(x: Float, y: Float){

        end(false)

        currentCurve += Vector2f(x, y)

        this.x = x
        this.y = y

    }

    fun end(closed: Boolean){

        if(currentCurve.isNotEmpty()){
            curves += SVGCurve(ArrayList(currentCurve), closed, z,
                if(currentFill) currentStyle.fill!! else currentStyle.stroke!!,
                if(currentFill) 0f else currentStyle.strokeWidth)
            currentCurve.clear()
        }

    }

    fun close() = end(true)

}