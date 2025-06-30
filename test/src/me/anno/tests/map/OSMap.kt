package me.anno.tests.map

import me.anno.cache.ICacheData
import me.anno.utils.types.Floats.toRadians
import speiger.primitivecollections.LongToObjectHashMap
import kotlin.math.cos

class OSMap(s0: Int = 65536, s1: Int = 1024, s2: Int = 256) : ICacheData {
    var minLon = 0.0
    var minLat = 0.0
    var maxLon = 0.0
    var maxLat = 0.0
    val lonScale get() = 1.0 / (maxLon - minLon)
    val latScale get() = 1.0 / (maxLat - minLat)
    val scaleX get() = (maxLon - minLon) * cos((maxLat + minLat).toRadians() * 0.5f) / (maxLat - minLat)
    val nodes = LongToObjectHashMap<OSMNode>(s0)
    val ways = LongToObjectHashMap<OSMWay>(s1)
    val relations = LongToObjectHashMap<OSMRelation>(s2)
    override fun destroy() {}
}