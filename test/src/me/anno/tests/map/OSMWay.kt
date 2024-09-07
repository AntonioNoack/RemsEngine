package me.anno.tests.map

/**
 * <way id="166332654" visible="true" version="2" changeset="19164882" timestamp="2013-11-28T18:11:02Z" user="cMartin" uid="128287">
 *  <nd ref="1777867490"/>
 *  <nd ref="1777867493"/>
 *  <nd ref="1777867505"/>
 *  <nd ref="1777867504"/>
 *  <nd ref="1777867511"/>
 *  <nd ref="1777867510"/>
 *  <nd ref="1777867490"/>
 *  <tag k="addr:city" v="Jena"/>
 *  <tag k="addr:country" v="DE"/>
 *  <tag k="addr:housenumber" v="14"/>
 *  <tag k="addr:postcode" v="07749"/>
 *  <tag k="addr:street" v="Am Burggarten"/>
 *  <tag k="building" v="yes"/>
 * </way>
 * */
class OSMWay(
    val nodes: List<OSMNode>,
    val minLon: Float,
    val minLat: Float,
    val maxLon: Float,
    val maxLat: Float,
    val tags: HashMap<String, String>?,
    var used: Boolean = false,
    var role: String = ""
)
