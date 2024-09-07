package me.anno.tests.map

class OSMRelation(
    val waysByType: Map<String, List<OSMWay>>,
    val nodesByType: Map<String, List<OSMNode>>,
    val tags: HashMap<String, String>?,
)