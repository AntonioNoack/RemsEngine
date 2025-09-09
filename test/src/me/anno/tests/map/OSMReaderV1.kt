package me.anno.tests.map

import me.anno.io.generic.ObjectReader.Companion.TAG_NAME
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.structures.lists.Lists.count2
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToLong.getLong
import me.anno.utils.types.Strings.toLong
import java.io.InputStream

object OSMReaderV1 {
    /**
     * easy reader implementation;
     * using quite a lot of memory
     * */
    fun readOSM1(input: InputStream, shallReadTags: Boolean = false, map: OSMap = OSMap()): OSMap {

        val xml = XMLReader(input.reader()).readXMLNode()!!
        val boundsNode = xml.children.first { it is XMLNode && it.type == "bounds" } as XMLNode
        map.minLon = getDouble(boundsNode["minlon"])
        map.minLat = getDouble(boundsNode["minlat"])
        map.maxLon = getDouble(boundsNode["maxlon"])
        map.maxLat = getDouble(boundsNode["maxlat"])

        val facX = map.lonScale
        val facY = map.latScale

        fun readTags(child: XMLNode): HashMap<String, String>? {
            if (!shallReadTags) return null
            val count = child.children.count2 { it is XMLNode && it.type == "tag" }
            return if (count > 0) {
                val tags = HashMap<String, String>(count)
                for (tag in child.children) {
                    tag as? XMLNode ?: continue
                    if (tag.type != "tag") continue
                    val k = tag["k"] ?: continue
                    val v = tag["v"] ?: continue
                    tags[k] = v
                }
                tags
            } else null
        }

        for (child in xml.children) {
            child as? XMLNode ?: continue
            if (child.type == "node") {
                val id = (child["id"] ?: continue).toLong()
                val lat = (getDouble(child["lat"]) - map.minLat) * facY - 1.0
                val lon = (getDouble(child["lon"]) - map.minLon) * facX - 1.0
                map.nodes[id] = OSMNode(-lat.toFloat(), lon.toFloat(), readTags(child))
            }
        }

        for (child in xml.children) {
            child as? XMLNode ?: continue
            if (child.type == "way") {
                val id = (child["id"] ?: continue).toLong()
                val nds = ArrayList<OSMNode>(child.children.count2 { it is XMLNode && it.type == "nd" })
                for (nd in child.children) {
                    nd as? XMLNode ?: continue
                    if (nd.type == "nd") {
                        val node = map.nodes[getLong(nd["ref"])]!!
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
            child as? XMLNode ?: continue
            if (child.type == "relation") {
                val id = (child["id"] ?: continue).toLong()
                val members = child.children.filterIsInstance<XMLNode>()
                    .filter { it.type == "member" }
                map.relations[id] = OSMRelation(
                    members.filter { it["type"] == "way" }
                        .groupBy { it["role"] as String }
                        .mapValues { m ->
                            m.value.mapNotNull {
                                val element = map.ways[getLong(it["ref"])]
                                element?.used = true
                                element
                            }
                        },
                    members.filter { it["type"] == "node" }
                        .groupBy { it["role"] as String }
                        .mapValues { m ->
                            m.value.mapNotNull {
                                val element = map.nodes[getLong(it["ref"])]
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