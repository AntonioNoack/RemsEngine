package me.anno.tests.map

/**
 * <node id="240090160" visible="true" version="33" changeset="107793766" timestamp="2021-07-11T18:36:51Z" user="Zinoural" uid="6515906" lat="50.9281717" lon="11.5879359">
 * */
class OSMNode(
    val relLat: Float, val relLon: Float,
    val tags: Map<String, String>?,
    var used: Boolean = false,
    var role: String = ""
)