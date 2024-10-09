package me.anno.tests.map

import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.Strings.toDouble
import me.anno.utils.types.Strings.toLong
import java.io.InputStream

object OSMReaderV1 {
    /**
     * easy reader implementation;
     * using quite a lot of memory
     * */
    fun readOSM1(input: InputStream, shallReadTags: Boolean = false, map: OSMap = OSMap()): OSMap {

        val xml = XMLReader().read(input.reader()) as XMLNode
        val boundsNode = xml.children.first { it is XMLNode && it.type == "bounds" } as XMLNode
        map.minLon = boundsNode["minlon"]!!.toDouble()
        map.minLat = boundsNode["minlat"]!!.toDouble()
        map.maxLon = boundsNode["maxlon"]!!.toDouble()
        map.maxLat = boundsNode["maxlat"]!!.toDouble()

        val facX = map.lonScale
        val facY = map.latScale

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
                val lat = (child["lat"]!!.toDouble() - map.minLat) * facY - 1.0
                val lon = (child["lon"]!!.toDouble() - map.minLon) * facX - 1.0
                map.nodes[id] = OSMNode(-lat.toFloat(), lon.toFloat(), readTags(child))
            }
        }

        for (child in xml.children) {
            if (child is XMLNode && child.type == "way") {
                val id = (child["id"] ?: continue).toLong()
                val nds = ArrayList<OSMNode>(child.children.count2 { it is XMLNode && it.type == "nd" })
                for (nd in child.children) {
                    if (nd is XMLNode && nd.type == "nd") {
                        val node = map.nodes[nd["ref"]!!.toLong()]!!
                        nds.add(node)
                        node.used = true
                    }
                }
                val minLon = nds.minOf { it.relLon }
                val maxLon = nds.maxOf { it.relLon }
                val minLat = nds.minOf { it.relLat }
                val maxLat = nds.maxOf { it.relLat }
                map.ways[id] = OSMWay(nds, minLon, minLat, maxLon, maxLat, readTags(child))
            }
        }

        for (child in xml.children) {
            if (child is XMLNode && child.type == "relation") {
                val id = (child["id"] ?: continue).toLong()
                val members = child.children.filterIsInstance<XMLNode>()
                    .filter { it.type == "member" }
                map.relations[id] = OSMRelation(
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
}