package me.anno.tests.map

import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.ui.UIColors
import me.anno.ui.base.groups.MapPanel
import me.anno.utils.Color.withAlpha
import me.anno.utils.types.Floats.roundToIntOr
import org.joml.Vector2d

// todo draw street names, and all extra information :)

class OSMPanelV1(val map: OSMap) : MapPanel(style) {

    init {
        minScale.set(1.0)
        maxScale.set(1e6)
        teleportScaleTo(Vector2d(250.0))
    }

    val minSize = 3f

    val scaleX = map.scaleX

    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        // draw all points
        var v = DrawRectangles.startBatch()
        val minLon = (windowToCoordsX(x0.toDouble()) / scaleX).toFloat()
        val maxLon = (windowToCoordsX(x1.toDouble()) / scaleX).toFloat()
        val minLat = windowToCoordsY(y0.toDouble()).toFloat()
        val maxLat = windowToCoordsY(y1.toDouble()).toFloat()
        for (node in map.nodes.values) {
            drawNode(node, minLon, minLat, maxLon, maxLat, UIColors.axisWColor)
        }
        for (relation in map.relations.values) {
            for (nodes2 in relation.nodesByType.values) {
                // to do color by type
                for (node in nodes2) {
                    drawNode(node, minLon, minLat, maxLon, maxLat, UIColors.axisYColor)
                }
            }
        }
        DrawRectangles.finishBatch(v)
        v = DrawCurves.lineBatch.start()
        // draw all lines
        for (way in map.ways.values) {
            drawWay(way, minLon, minLat, maxLon, maxLat, UIColors.axisXColor)
        }
        for (relation in map.relations.values) {
            for (ways2 in relation.waysByType.values) {
                // to do color by type
                for (way in ways2) {
                    drawWay(way, minLon, minLat, maxLon, maxLat, UIColors.axisZColor)
                }
            }
        }
        val x0f = coordsToWindowX(-scaleX).toFloat()
        val x1f = coordsToWindowX(+scaleX).toFloat()
        val y0f = coordsToWindowY(-1.0).toFloat()
        val y1f = coordsToWindowY(+1.0).toFloat()
        val bg = backgroundColor.withAlpha(0)
        drawLine(x0f, y0f, x1f, y0f, 1f, -1, bg, false)
        drawLine(x1f, y0f, x1f, y1f, 1f, -1, bg, false)
        drawLine(x1f, y1f, x0f, y1f, 1f, -1, bg, false)
        drawLine(x0f, y1f, x0f, y0f, 1f, -1, bg, false)
        DrawCurves.lineBatch.finish(v)
    }

    fun drawNode(node: OSMNode, minLon: Float, minLat: Float, maxLon: Float, maxLat: Float, color: Int) {
        val lon = node.relLon
        val lat = node.relLat
        if (!node.used && lon in minLon..maxLon && lat in minLat..maxLat) {
            val x = coordsToWindowX(lon * scaleX)
            val y = coordsToWindowY(lat.toDouble())
            val xi = x.roundToIntOr()
            val yi = y.roundToIntOr()
            drawRect(xi - 1, yi - 1, 3, 3, color)
        }
    }

    fun drawWay(way: OSMWay, minLon: Float, minLat: Float, maxLon: Float, maxLat: Float, color: Int) {
        if (way.minLon < maxLon && way.minLat < maxLat && way.maxLon > minLon && way.maxLat > minLat && // within bounds
            ((way.maxLon - way.minLon) * width > minSize * (maxLon - minLon) || // larger than minimum size
                    (way.maxLat - way.minLat) * height > minSize * (maxLat - minLat))
        ) {
            val nds = way.nodes
            val nd0 = nds[0]
            var x0i = coordsToWindowX(nd0.relLon * scaleX).toFloat()
            var y0i = coordsToWindowY(nd0.relLat.toDouble()).toFloat()
            var inside0 = nd0.relLon in -1f..1f && nd0.relLat in -1f..1f
            val bg = backgroundColor.withAlpha(0)
            for (i in 1 until nds.size) {
                val nd1 = nds[i]
                val inside1 = nd1.relLon in -1f..1f && nd1.relLat in -1f..1f
                val x1i = coordsToWindowX(nd1.relLon * scaleX).toFloat()
                val y1i = coordsToWindowY(nd1.relLat.toDouble()).toFloat()
                if (inside0 || inside1) {
                    drawLine(x0i, y0i, x1i, y1i, 1f, color, bg, true)
                }
                x0i = x1i
                y0i = y1i
                inside0 = inside1
            }
        }
    }
}