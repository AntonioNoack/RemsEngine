package me.anno.tests.map

import me.anno.io.xml.generic.XMLScanner
import me.anno.utils.types.Strings.toDouble
import me.anno.utils.types.Strings.toLong
import java.io.InputStream

object OSMReaderV2 {
    /**
     * more complicated reader implementation;
     * twice as fast, and probably using much less memory
     * */
    fun readOSM2(file: InputStream, shallReadTags: Boolean = false, map: OSMap = OSMap()): OSMap {

        // potential improvement: don't convert toString before toDouble/toLong

        val tags = HashMap<String, String>(64)

        var id = 0L
        var relLat = 0f
        var relLon = 0f

        var lonScale = 1.0
        var latScale = 1.0
        var minLon = 0.0
        var minLat = 0.0

        val mapNodes = ArrayList<OSMNode>(64)

        var tagKey = ""
        var tagValue = ""

        val relWays = ArrayList<OSMWay>(64)
        val relNodes = ArrayList<OSMNode>(64)

        var memType = ""
        var memRef = 0L
        var memRole = ""

        var ref = 0L

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

        fun latTo01(lat: Double): Float {
            return ((lat - minLat) * latScale).toFloat()
        }

        fun lonTo01(lon: Double): Float {
            return ((lon - minLon) * lonScale).toFloat()
        }

        object : XMLScanner(file.reader()) {
            override fun onStart(depth: Int, type: CharSequence): Boolean {
                return shallReadTags || type != "tag"
            }

            override fun onEnd(depth: Int, type: CharSequence) {
                when (type) {
                    "bounds" -> if (depth == 1) {
                        lonScale = map.lonScale
                        latScale = map.latScale
                        minLon = map.minLon
                        minLat = map.minLat
                    }
                    "node" -> map.nodes[id] = OSMNode(relLat, relLon, readTags2())
                    "way" -> {
                        map.ways[id] = OSMWay(
                            mapNodes.toList(),
                            mapNodes.minOf { it.relLon },
                            mapNodes.minOf { it.relLat },
                            mapNodes.maxOf { it.relLon },
                            mapNodes.maxOf { it.relLat }, readTags2()
                        )
                        mapNodes.clear()
                    }
                    "tag" -> tags[tagKey] = tagValue
                    "relation" -> {
                        map.relations[id] = OSMRelation(
                            relWays.groupBy { it.role },
                            relNodes.groupBy { it.role },
                            readTags2()
                        )
                        relWays.clear()
                        relNodes.clear()
                    }
                    "nd" -> {
                        if (ref != 0L) {
                            val node = map.nodes[ref]!!
                            node.used = true
                            mapNodes.add(node)
                            ref = 0L
                        } else {
                            relNodes.add(OSMNode(relLat, relLon, emptyMap()))
                        }
                    }
                    "member" -> {
                        when (memType) {
                            "way" -> {
                                var way = map.ways[memRef]
                                if (way != null) {
                                    way.used = true
                                    way.role = memRole
                                    relWays.add(way)
                                } else {
                                    way = OSMWay(
                                        ArrayList(relNodes),
                                        relNodes.minOf { it.relLon },
                                        relNodes.minOf { it.relLat },
                                        relNodes.maxOf { it.relLon },
                                        relNodes.maxOf { it.relLat },
                                        null, true, memRole
                                    )
                                    map.ways[memRef] = way
                                    relNodes.clear()
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
            }

            override fun onContent(depth: Int, type: CharSequence, value: CharSequence) {}

            override fun onAttribute(depth: Int, type: CharSequence, key: CharSequence, value: CharSequence) {
                val k = key
                val v = value
                when (type) {
                    "bounds" -> if (depth == 1) {
                        when (k) {
                            "minlon" -> map.minLon = v.toDouble()
                            "minlat" -> map.minLat = v.toDouble()
                            "maxlon" -> map.maxLon = v.toDouble()
                            "maxlat" -> map.maxLat = v.toDouble()
                        }
                    }
                    "node" -> when (k) {
                        "id" -> id = v.toLong()
                        "lat" -> relLat = latTo01(v.toDouble())
                        "lon" -> relLon = lonTo01(v.toDouble())
                    }
                    "way", "relation" -> if (k == "id") id = v.toLong()
                    "nd" -> when (k) {
                        "ref" -> ref = v.toLong()
                        "lat" -> relLat = latTo01(v.toDouble())
                        "lon" -> relLon = lonTo01(v.toDouble())
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
            }
        }.scan()

        println("loaded ${map.nodes.size} nodes + ${map.ways.size} ways + ${map.relations.size} relations")

        return map
    }
}