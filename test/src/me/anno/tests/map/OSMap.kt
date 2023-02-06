package me.anno.tests.map

import me.anno.config.DefaultConfig.style
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawRectangles.finishBatch
import me.anno.gpu.drawing.DrawRectangles.startBatch
import me.anno.input.Input
import me.anno.io.xml.XMLNode
import me.anno.io.xml.XMLReader
import me.anno.io.xml.XMLScanner
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestStudio.Companion.testUI3
import me.anno.utils.Color.withAlpha
import me.anno.utils.OS.downloads
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Strings.toDouble
import me.anno.utils.types.Strings.toLong
import java.io.InputStream
import kotlin.math.cos

// <node id="240090160" visible="true" version="33" changeset="107793766" timestamp="2021-07-11T18:36:51Z" user="Zinoural" uid="6515906" lat="50.9281717" lon="11.5879359">
class Node(
    val lat: Float, val lon: Float,
    val tags: HashMap<String, String>?,
    var used: Boolean = false,
    var role: String = ""
)

// <way id="166332654" visible="true" version="2" changeset="19164882" timestamp="2013-11-28T18:11:02Z" user="cMartin" uid="128287">
//  <nd ref="1777867490"/>
//  <nd ref="1777867493"/>
//  <nd ref="1777867505"/>
//  <nd ref="1777867504"/>
//  <nd ref="1777867511"/>
//  <nd ref="1777867510"/>
//  <nd ref="1777867490"/>
//  <tag k="addr:city" v="Jena"/>
//  <tag k="addr:country" v="DE"/>
//  <tag k="addr:housenumber" v="14"/>
//  <tag k="addr:postcode" v="07749"/>
//  <tag k="addr:street" v="Am Burggarten"/>
//  <tag k="building" v="yes"/>
// </way>
class Way(
    val nodes: Array<Node>,
    val minLon: Float,
    val minLat: Float,
    val maxLon: Float,
    val maxLat: Float,
    val tags: HashMap<String, String>?,
    var used: Boolean = false,
    var role: String = ""
)

class Relation(
    val waysByType: Map<String, List<Way>>,
    val nodesByType: Map<String, List<Node>>,
    val tags: HashMap<String, String>?,
)

class OSMap(s0: Int = 65536, s1: Int = 1024, s2: Int = 256) {
    var minX = 0.0
    var minY = 0.0
    var maxX = 0.0
    var maxY = 0.0
    val facX get() = 2.0 / (maxX - minX)
    val facY get() = 2.0 / (maxY - minY)
    val scaleX get() = (maxX - minX) * cos((maxY + minY).toRadians() * 0.5f) / (maxY - minY)
    val nodes = HashMap<Long, Node>(s0)
    val ways = HashMap<Long, Way>(s1)
    val relations = HashMap<Long, Relation>(s2)
}

/**
 * easy reader implementation;
 * using quite a lot of memory
 * */
fun readOSM0(input: InputStream, shallReadTags: Boolean = false, map: OSMap = OSMap()): OSMap {

    val xml = XMLReader().parse(input) as XMLNode
    val boundsNode = xml.children.first { it is XMLNode && it.type == "bounds" } as XMLNode
    map.minX = boundsNode["minlon"]!!.toDouble()
    map.minY = boundsNode["minlat"]!!.toDouble()
    map.maxX = boundsNode["maxlon"]!!.toDouble()
    map.maxY = boundsNode["maxlat"]!!.toDouble()

    val facX = map.facX
    val facY = map.facY

    val n0 = Node(0f, 0f, null)

    fun readTags(child: XMLNode): HashMap<String, String>? {
        if (!shallReadTags) return null
        val count = child.children.count2 { it is XMLNode && it.type == "tag" }
        return if (count > 0) {
            val tags = HashMap<String, String>(count)
            for (tag in child.children) {
                if (tag !is XMLNode || tag.type != "tag") continue
                val k = tag["k"] ?: continue
                val v = tag["v"] ?: continue
                tags[k] = v
            }
            tags
        } else null
    }

    for (child in xml.children) {
        if (child is XMLNode && child.type == "node") {
            val id = (child["id"] ?: continue).toLong()
            val lat = (child["lat"]!!.toDouble() - map.minY) * facY - 1.0
            val lon = (child["lon"]!!.toDouble() - map.minX) * facX - 1.0
            map.nodes[id] = Node(-lat.toFloat(), lon.toFloat(), readTags(child))
        }
    }

    for (child in xml.children) {
        if (child is XMLNode && child.type == "way") {
            val id = (child["id"] ?: continue).toLong()
            val nds = Array(child.children.count2 { it is XMLNode && it.type == "nd" }) { n0 }
            var i = 0
            for (nd in child.children) {
                if (nd is XMLNode && nd.type == "nd") {
                    val node = map.nodes[nd["ref"]!!.toLong()]!!
                    nds[i++] = node
                    node.used = true
                }
            }
            val minLon = nds.minOf { it.lon }
            val maxLon = nds.maxOf { it.lon }
            val minLat = nds.minOf { it.lat }
            val maxLat = nds.maxOf { it.lat }
            map.ways[id] = Way(nds, minLon, minLat, maxLon, maxLat, readTags(child))
        }
    }

    for (child in xml.children) {
        if (child is XMLNode && child.type == "relation") {
            val id = (child["id"] ?: continue).toLong()
            val members = child.children.filterIsInstance<XMLNode>()
                .filter { it.type == "member" }
            map.relations[id] = Relation(
                members.filter { it["type"] == "way" }
                    .groupBy { it["role"]!! }
                    .mapValues { m ->
                        m.value.mapNotNull {
                            val element = map.ways[it["ref"]!!.toLong()]
                            element?.used = true
                            element
                        }
                    },
                members.filter { it["type"] == "node" }
                    .groupBy { it["role"]!! }
                    .mapValues { m ->
                        m.value.mapNotNull {
                            val element = map.nodes[it["ref"]!!.toLong()]
                            element?.used = true
                            element
                        }
                    },
                readTags(child)
            )
        }
    }

    return map

}

/**
 * more complicated reader implementation;
 * twice as fast, and probably using much less memory
 * */
fun readOSM1(file: InputStream, shallReadTags: Boolean = false, map: OSMap = OSMap()): OSMap {

    // potential improvement: don't convert toString before toDouble/toLong

    val tags = HashMap<String, String>(64)

    var id = 0L
    var lat = 0f
    var lon = 0f

    var facX = 1.0
    var facY = 1.0
    var minX = 0.0
    var minY = 0.0

    val mapNodes = ArrayList<Node>(64)

    var tagKey = ""
    var tagValue = ""

    val relWays = ArrayList<Way>(64)
    val relNodes = ArrayList<Node>(64)

    var memType = ""
    var memRef = 0L
    var memRole = ""

    fun readTags2(): HashMap<String, String>? {
        return if (tags.isNotEmpty()) {
            val clone = HashMap(tags)
            tags.clear()
            clone
        } else null
    }

    val types = hashMapOf(
        "way" to "way",
        "node" to "node",
        "relation" to "relation"
    )

    val roles = listOf(
        "forward", "inner", "outer", "stop", "platform", "backward", "",
        "main_stream", "side_stream", "route_marker", "guidepost",
        "from", "to", "via", "part"
    ).associateWith { it }

    XMLScanner().parse(file, { type ->
        shallReadTags || type != "tag"
    }, { type ->
        when (type) {
            "bounds" -> {
                facX = map.facX
                facY = map.facY
                minX = map.minX
                minY = map.minY
            }
            "node" -> map.nodes[id] = Node(-lat, lon, readTags2())
            "way" -> {
                map.ways[id] = Way(
                    mapNodes.toTypedArray(),
                    mapNodes.minOf { it.lon },
                    mapNodes.minOf { it.lat },
                    mapNodes.maxOf { it.lon },
                    mapNodes.maxOf { it.lat }, readTags2()
                )
                mapNodes.clear()
            }
            "tag" -> tags[tagKey] = tagValue
            "relation" -> {
                map.relations[id] = Relation(
                    relWays.groupBy { it.role },
                    relNodes.groupBy { it.role },
                    readTags2()
                )
                relWays.clear()
                relNodes.clear()
            }
            "member" -> {
                when (memType) {
                    "way" -> {
                        val way = map.ways[memRef]
                        if (way != null) {
                            way.used = true
                            way.role = memRole
                            relWays.add(way)
                        }
                    }
                    "node" -> {
                        val node = map.nodes[memRef]
                        if (node != null) {
                            node.used = true
                            node.role = memRole
                            relNodes.add(node)
                        }
                    }
                }
            }
        }
    }, { type, k, v ->
        when (type) {
            "bounds" -> when (k) {
                "minlon" -> map.minX = v.toDouble()
                "minlat" -> map.minY = v.toDouble()
                "maxlon" -> map.maxX = v.toDouble()
                "maxlat" -> map.maxY = v.toDouble()
            }
            "node" -> when (k) {
                "id" -> id = v.toLong()
                "lat" -> lat = ((v.toDouble() - minY) * facY - 1.0).toFloat()
                "lon" -> lon = ((v.toDouble() - minX) * facX - 1.0).toFloat()
            }
            "way", "relation" -> if (k == "id") id = v.toLong()
            "nd" -> if (k == "ref") {
                val node = map.nodes[v.toLong()]!!
                node.used = true
                mapNodes.add(node)
            }
            "tag" -> when (k) {
                "key" -> tagKey = v.toString()
                "value" -> tagValue = v.toString()
            }
            "member" -> when (k) {
                "ref" -> memRef = v.toLong()
                "type" -> memType = types[v] ?: ""
                "role" -> memRole = roles[v] ?: v.toString()
            }
        }
    })

    return map

}

fun main() {

    // load OSM data, and visualize it in real time
    // https://www.openstreetmap.org/export
    // might be useful to somebody...

    // todo draw street names, and all extra information :)

    val file = downloads.getChild("map2.osm")
    val data = file.readBytesSync()

    val tags = false
    /*val clock = Clock()

    clock.benchmark(5, 20, "map0") {
        readOSM0(data.inputStream(), tags)
    }

    // 2.3x faster :)
    clock.benchmark(5, 20, "map1") {
        readOSM1(data.inputStream(), tags)
    }*/

    testUI3 {
        object : MapPanel(style) {

            init {
                minScale = 1.0
                maxScale = 1e6
                targetScale = 250.0
            }

            val minSize = 3f

            val map = readOSM1(data.inputStream(), tags)

            val scaleX = map.scaleX

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)
                // draw all points
                startBatch()
                val minLon = (windowToCoordsX(x0.toDouble()) / scaleX).toFloat()
                val maxLon = (windowToCoordsX(x1.toDouble()) / scaleX).toFloat()
                val minLat = windowToCoordsY(y0.toDouble()).toFloat()
                val maxLat = windowToCoordsY(y1.toDouble()).toFloat()
                for (node in map.nodes.values) {
                    drawNode(node, minLon, minLat, maxLon, maxLat, -1)
                }
                for (relation in map.relations.values) {
                    for (nodes2 in relation.nodesByType.values) {
                        // to do color by type
                        for (node in nodes2) {
                            drawNode(node, minLon, minLat, maxLon, maxLat, -1)
                        }
                    }
                }
                finishBatch()
                val lineBatch = !Input.isShiftDown
                if (lineBatch) DrawCurves.lineBatch.start()
                // draw all lines
                for (way in map.ways.values) {
                    drawWay(way, minLon, minLat, maxLon, maxLat, -1)
                }
                for (relation in map.relations.values) {
                    for (ways2 in relation.waysByType.values) {
                        // to do color by type
                        for (way in ways2) {
                            drawWay(way, minLon, minLat, maxLon, maxLat, -1)
                        }
                    }
                }
                /*val x0f = coordsToWindowX(-scaleX).toFloat()
                val x1f = coordsToWindowX(+scaleX).toFloat()
                val y0f = coordsToWindowY(-1.0).toFloat()
                val y1f = coordsToWindowY(+1.0).toFloat()
                val bg = backgroundColor.withAlpha(0)
                drawLine(x0f, y0f, x1f, y0f, 1f, -1, bg, false)
                drawLine(x1f, y0f, x1f, y1f, 1f, -1, bg, false)
                drawLine(x1f, y1f, x0f, y1f, 1f, -1, bg, false)
                drawLine(x0f, y1f, x0f, y0f, 1f, -1, bg, false)*/
                if (lineBatch) DrawCurves.lineBatch.finish()
            }

            fun drawNode(node: Node, minLon: Float, minLat: Float, maxLon: Float, maxLat: Float, color: Int) {
                val lon = node.lon
                val lat = node.lat
                if (!node.used && lon in minLon..maxLon && lat in minLat..maxLat) {
                    val x = coordsToWindowX(lon * scaleX)
                    val y = coordsToWindowY(lat.toDouble())
                    val xi = x.toInt()
                    val yi = y.toInt()
                    drawRect(xi, yi, 1, 1, color)
                }
            }

            fun drawWay(way: Way, minLon: Float, minLat: Float, maxLon: Float, maxLat: Float, color: Int) {
                if (way.minLon < maxLon && way.minLat < maxLat && way.maxLon > minLon && way.maxLat > minLat && // within bounds
                    ((way.maxLon - way.minLon) * w > minSize * (maxLon - minLon) || // larger than minimum size
                            (way.maxLat - way.minLat) * h > minSize * (maxLat - minLat))
                ) {
                    val nds = way.nodes
                    val nd0 = nds[0]
                    var x0i = coordsToWindowX(nd0.lon * scaleX).toFloat()
                    var y0i = coordsToWindowY(nd0.lat.toDouble()).toFloat()
                    var inside0 = nd0.lon in -1f..1f && nd0.lat in -1f..1f
                    val bg = backgroundColor.withAlpha(0)
                    for (i in 1 until nds.size) {
                        val nd1 = nds[i]
                        val inside1 = nd1.lon in -1f..1f && nd1.lat in -1f..1f
                        val x1i = coordsToWindowX(nd1.lon * scaleX).toFloat()
                        val y1i = coordsToWindowY(nd1.lat.toDouble()).toFloat()
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
    }

}