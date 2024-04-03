package me.anno.tests.ui

import me.anno.cache.CacheSection
import me.anno.config.DefaultConfig.style
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.io.Streams.readText
import me.anno.io.Streams.writeString
import me.anno.io.config.ConfigBasics
import me.anno.io.files.WebRef.Companion.encodeURIComponent
import me.anno.tests.map.OSMap
import me.anno.tests.map.readOSM1
import me.anno.tests.ui.OSMMapCache.getMapData
import me.anno.ui.Style
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
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
 * not yet working!
 * */
fun main() {
    disableRenderDoc()
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

object OSMMapCache : CacheSection("OSMMapData") {
    val hdb = HierarchicalDatabase(
        "OSMMapData", ConfigBasics.cacheFolder.getChild("OSMap"),
        20_000_000, 10_000, 7 * 24 * 3600 * 1000
    )

    fun getMapData(bounds: AABBd, level: Int, async: Boolean): OSMap? {
        val query = "" +
                "[bbox:${bounds.maxX},${bounds.maxY},${bounds.minX},${bounds.minY}]\n" +
                "[out:xml]\n" +
                "[timeout:90]\n" + // seconds
                ";\n" +
                "way(${bounds.minX},${bounds.minY},${bounds.maxX},${bounds.maxY});\n" +
                "node(${bounds.minX},${bounds.minY},${bounds.maxX},${bounds.maxY});\n" +
                "relation(${bounds.minX},${bounds.minY},${bounds.maxX},${bounds.maxY});\n" +
                "out geom;"
        return getDualEntry(bounds, level, 10_000, async) { _, _ ->
            // to do make this fully async?
            var result: OSMap? = null
            val path = listOf("${bounds.minX}", "${bounds.minY}")
            val hash = query.hashCode().toLong().and(0xffffffff)
            hdb.get(path, hash, false) { bytes ->
                if (bytes != null) {
                    result = readOSM1(bytes.stream())
                }
            }
            if (result != null) {
                // good, we have data
                val res = result!!
                println("got data from db, ${res.nodes.size} + ${res.ways.size} + ${res.relations.size}")
            } else {
                println("making request $query")
                limit.acquire()
                val con = URL(MAIN_URL).openConnection() as HttpURLConnection
                con.doOutput = true
                con.outputStream.writeString("data=${encodeURIComponent(query)}")
                if (con.responseCode == 200) {
                    val bytes = con.inputStream.use { it.readBytes() }
                    result = readOSM1(ByteArrayInputStream(bytes))
                    hdb.put(path, hash, ByteSlice(bytes))
                    println("made successful request, ${bytes.size} bytes")
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
        } as? OSMap
    }

    fun getMapData(bounds: AABBd, async: Boolean): List<OSMap> {
        val dx = min(bounds.deltaX, bounds.deltaY)
        val level = round(log2(dx))
        val levelI = level.toInt()
        // calculate min/max coords
        val scale = 2.0.pow(level)
        val x0 = floor(bounds.minX / scale).toLong()
        val y0 = floor(bounds.minY / scale).toLong()
        val x1 = ceil(bounds.maxX / scale).toLong()
        val y1 = ceil(bounds.maxY / scale).toLong()
        val result = ArrayList<OSMap>(((x1 - x0) * (y1 - y0)).toInt())
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val i0 = x * scale
                val j0 = y * scale
                val piece = getMapData(
                    AABBd(i0, j0, 0.0, i0 + scale, j0 + scale, 0.0),
                    levelI, async
                )
                if (piece != null) {
                    println("got piece :)")
                    result.add(piece)
                } else println("no piece :/")
            }
        }
        return result
    }
}

class OSMMapPanel(style: Style) : MapPanel(style) {

    init {
        // todo we need non-uniform scale
        teleportMapTo(Vector2d(-96.323712, 30.618338))
        teleportScaleTo(1e5)
        minScale = 0.0
        maxScale = 1e16
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
        bounds.minY = windowToCoordsX(x0.toDouble())
        bounds.maxY = windowToCoordsX(x1.toDouble())
        bounds.minX = windowToCoordsY(y0.toDouble())
        bounds.maxX = windowToCoordsY(y1.toDouble())
        val batch = DrawCurves.lineBatch.start()
        for (piece in getMapData(bounds, true)) {
            // todo draw map portion
            piece.minLon
            piece.minLat
            piece.maxLon
            piece.maxLat
            for ((k, way) in piece.ways) {
                val nodes = way.nodes
                val n0 = nodes.firstOrNull() ?: continue
                var nx0 = coordsToWindowX(n0.relLon)
                var ny0 = coordsToWindowY(n0.relLat)
                for (i in 1 until nodes.size) {
                    val n1 = nodes[i]
                    val nx1 = coordsToWindowX(n1.relLon)
                    val ny1 = coordsToWindowY(n1.relLat)
                    println("line: $nx0-$nx1, $ny0-$ny1")
                    DrawCurves.drawLine(nx0, ny0, nx1, ny1, 1f, white, backgroundColor.withAlpha(0), false)
                    nx0 = nx1
                    ny0 = ny1
                }
            }
        }
        DrawCurves.lineBatch.finish(batch)
    }
}