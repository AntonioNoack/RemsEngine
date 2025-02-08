package me.anno.tests.map

import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.maths.Maths.mix
import me.anno.tests.map.OSMMapCache.getMapData
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import org.joml.AABBd
import org.joml.Vector2d

class OSMPanelV2(style: Style) : MapPanel(style) {

    init {
        // todo we need non-uniform scale
        teleportMapTo(Vector2d(-74.0107088, 40.7090632))
        teleportScaleTo(Vector2d(1e5))
        minScale.set(1e-16)
        maxScale.set(1e16)
    }

    val bounds = AABBd()
    override fun draw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.draw(x0, y0, x1, y1)
        for (x in listOf(x0, x1)) {
            for (y in listOf(y0, y1)) {
                drawSimpleTextCharByChar(
                    x, y, 1, "${windowToCoordsY(y.toDouble())}/${windowToCoordsX(x.toDouble())}",
                    if (x == x0) AxisAlignment.MIN else AxisAlignment.MAX,
                    if (y == y0) AxisAlignment.MIN else AxisAlignment.MAX
                )
            }
        }
        bounds.clear()
        bounds.union(windowToCoordsX(x0.toDouble()), windowToCoordsY(y0.toDouble()), 0.0)
        bounds.union(windowToCoordsX(x1.toDouble()), windowToCoordsY(y1.toDouble()), 0.0)
        val batch = DrawCurves.lineBatch.start()
        val batch1 = DrawRectangles.startBatch()
        for ((bounds, piece) in getMapData(bounds, true)) {
            drawMapTile(bounds)
            drawMapTile(piece)
            /*for ((_, way) in piece.ways) {
                drawWay(piece, way, -1)
            }*/
            for ((_, rel) in piece.relations) {
                for ((type, ways) in rel.waysByType) {
                    val color = (0x777777 or type.hashCode()) or black
                    for (way in ways) {
                        drawWay(piece, way, color)
                    }
                }
                for ((type, nodes) in rel.nodesByType) {
                    val color = (0x777777 or type.hashCode()) or black
                    for (node in nodes) {
                        drawNode(piece, node, color)
                    }
                }
            }
        }
        DrawCurves.lineBatch.finish(batch)
        DrawRectangles.finishBatch(batch1)
    }

    private fun drawWay(piece: OSMap, way: OSMWay, color: Int) {
        val bg = backgroundColor.withAlpha(0)
        val nodes = way.nodes
        val n0 = nodes.firstOrNull() ?: return
        var nx0 = coordsToWindowX(mix(piece.minLon, piece.maxLon, n0.relLon.toDouble())).toFloat()
        var ny0 = coordsToWindowY(mix(piece.minLat, piece.maxLat, n0.relLat.toDouble())).toFloat()
        for (i in 1 until nodes.size) {
            val n1 = nodes[i]
            val nx1 = coordsToWindowX(mix(piece.minLon, piece.maxLon, n1.relLon.toDouble())).toFloat()
            val ny1 = coordsToWindowY(mix(piece.minLat, piece.maxLat, n1.relLat.toDouble())).toFloat()
            DrawCurves.drawLine(nx0, ny0, nx1, ny1, 1f, color, bg, false)
            nx0 = nx1
            ny0 = ny1
        }
    }

    private fun drawNode(piece: OSMap, node: OSMNode, color: Int) {
        val nx0 = coordsToWindowX(mix(piece.minLon, piece.maxLon, node.relLon.toDouble())).toInt()
        val ny0 = coordsToWindowY(mix(piece.minLat, piece.maxLat, node.relLat.toDouble())).toInt()
        DrawRectangles.drawRect(nx0 - 2, ny0 - 2, 5, 5, color)
    }

    private fun drawMapTile(piece: OSMap) {
        val nx0 = coordsToWindowX(piece.minLon).toFloat()
        val ny0 = coordsToWindowY(piece.minLat).toFloat()
        val nx1 = coordsToWindowX(piece.maxLon).toFloat()
        val ny1 = coordsToWindowY(piece.maxLat).toFloat()
        val bg = backgroundColor.withAlpha(0)
        val cl = white
        DrawCurves.drawLine(nx0, ny0, nx0, ny1, 1f, cl, bg, false)
        DrawCurves.drawLine(nx0, ny1, nx1, ny1, 1f, cl, bg, false)
        DrawCurves.drawLine(nx1, ny1, nx1, ny0, 1f, cl, bg, false)
        DrawCurves.drawLine(nx1, ny0, nx0, ny0, 1f, cl, bg, false)
    }

    private fun drawMapTile(piece: AABBd) {
        val nx0 = coordsToWindowX(piece.minX).toFloat()
        val ny0 = coordsToWindowY(piece.minY).toFloat()
        val nx1 = coordsToWindowX(piece.maxX).toFloat()
        val ny1 = coordsToWindowY(piece.maxY).toFloat()
        val bg = backgroundColor.withAlpha(0)
        val cl = UIColors.midOrange
        DrawCurves.drawLine(nx0, ny0, nx0, ny1, 1f, cl, bg, false)
        DrawCurves.drawLine(nx0, ny1, nx1, ny1, 1f, cl, bg, false)
        DrawCurves.drawLine(nx1, ny1, nx1, ny0, 1f, cl, bg, false)
        DrawCurves.drawLine(nx1, ny0, nx0, ny0, 1f, cl, bg, false)
    }
}