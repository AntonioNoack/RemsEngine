package me.anno.tests.ui

import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.io.Streams.readText
import me.anno.io.Streams.writeString
import me.anno.io.config.ConfigBasics
import me.anno.io.files.WebRef.Companion.encodeURIComponent
import me.anno.maths.Maths.mix
import me.anno.tests.map.OSMNode
import me.anno.tests.map.OSMWay
import me.anno.tests.map.OSMap
import me.anno.tests.map.readOSM1
import me.anno.tests.ui.OSMMapCache.getMapData
import me.anno.ui.Style
import me.anno.ui.UIColors
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.files.Files.formatFileSize
import org.joml.AABBd
import org.joml.Vector2d
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Semaphore
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/**
 * todo hierarchical database is broken!!!
 * */
fun main() {
    // disableRenderDoc()
    testUI3("Open Street Maps", OSMMapPanel(style))
    // https://wiki.openstreetmap.org/wiki/Overpass_API#The_Programmatic_Query_Language_(OverpassQL)
    // 51.249,7.148,51.251,7.152) is minimum latitude, minimum longitude, maximum latitude, maximum longitude (or South-West-North-East)
    /*var result = await fetch(
    "https://overpass-api.de/api/interpreter",
    {
        method: "POST",
        body: "data="+ encodeURIComponent(`
            [bbox:30.618338,-96.323712,30.591028,-96.330826]
            [out:json]
            [timeout:90]
            ;
            (
                way
                    (
                         30.626917110746,
                         -96.348809105664,
                         30.634468750236,
                         -96.339893442898
                     );
            );
            out geom;
        `)
    },
).then(
    (data)=>data.json()
)
console.log(JSON.stringify(result , null, 2))*/
}

val MAIN_URL = "https://overpass-api.de/api/interpreter"

val limit = Semaphore(1)
val apiLimitMillis = 500L

data class Key(val xi: Long, val yi: Long, val level: Int)

object OSMMapCache : CacheSection("OSMMapData") {
    val hdb = HierarchicalDatabase(
        "OSMMapData", ConfigBasics.cacheFolder.getChild("OSMap"),
        20_000_000, 10_000, 7 * 24 * 3600 * 1000
    )

    fun getMapData(bounds: AABBd, key: Key, async: Boolean): OSMap? {
        val query = "" +
                "[bbox:${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX}]\n" +
                "[out:xml]\n" +
                "[timeout:90]\n" + // seconds
                ";\n" +
                "way(${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX});\n" +
                "node(${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX});\n" +
                "relation(${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX});\n" +
                "out geom;"
        return getEntry(key, 10_000, async) { key1 ->
            // to do make this fully async?
            var result: OSMap? = null
            val path = listOf(key1.xi.toString(), key1.yi.toString(), key1.level.toString())
            val hash = query.hashCode().toLong().and(0xffffffff)
            hdb.get(path, hash, false) { bytes ->
                if (bytes != null) {
                    result = readOSM1(bytes.stream())
                }
            }
            if (result == null) {
                limit.acquire()
                val con = URL(MAIN_URL).openConnection() as HttpURLConnection
                con.doOutput = true
                con.outputStream.writeString("data=${encodeURIComponent(query)}")
                if (con.responseCode == 200) {
                    val bytes = con.inputStream.use { it.readBytes() }
                    result = readOSM1(ByteArrayInputStream(bytes))
                    hdb.put(path, hash, ByteSlice(bytes))
                    println("made successful request, ${bytes.size.formatFileSize()}")
                    Thread.sleep(apiLimitMillis)
                    limit.release()
                } else {
                    Thread.sleep(apiLimitMillis)
                    limit.release()
                    println("Illegal response code: ${con.responseCode}")
                    println(con.errorStream.readText())
                    throw IOException()
                }
            }
            result!!
        }
    }

    fun getMapData(bounds: AABBd, async: Boolean): List<Pair<AABBd, OSMap>> {
        val dx = min(bounds.deltaX, bounds.deltaY)
        val level = round(log2(dx))
        val levelI = level.toInt()
        // calculate min/max coords
        val di = 2.0.pow(level)
        val x0 = floor(bounds.minX / di).toLong()
        val y0 = floor(bounds.minY / di).toLong()
        val x1 = ceil(bounds.maxX / di).toLong()
        val y1 = ceil(bounds.maxY / di).toLong()
        val result = ArrayList<Pair<AABBd, OSMap>>(((x1 - x0) * (y1 - y0)).toInt())
        for (yi in y0 until y1) {
            for (xi in x0 until x1) {
                val x = xi * di
                val y = yi * di
                val boundsI = AABBd(x, y, 0.0, x + di, y + di, 0.0)
                val piece = getMapData(boundsI, Key(xi, yi, levelI), async)
                if (piece != null) {
                    // println("got piece :)")
                    result.add(boundsI to piece)
                }// else println("no piece :/")
            }
        }
        return result
    }
}

class OSMMapPanel(style: Style) : MapPanel(style) {

    init {
        // todo we need non-uniform scale
        teleportMapTo(Vector2d(-74.0107088, 40.7090632))
        teleportScaleTo(Vector2d(1e5))
        minScale.set(1e-16)
        maxScale.set(1e16)
    }

    val bounds = AABBd()
    override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
        super.onDraw(x0, y0, x1, y1)
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