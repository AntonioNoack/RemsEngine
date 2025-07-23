package me.anno.tests.map

import me.anno.io.xml.generic.XMLScanner
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class XMLScannerTest {
    val source = """
        <?xml version="1.0" encoding="UTF-8"?>
<osm version="0.6">
 <bounds minlat="50.9728900" minlon="11.3098500" maxlat="50.9867200" maxlon="11.3435300"/>
 <node id="21441546" lat="50.9839245" lon="11.3309530">
  <tag k="barrier" v="bollard"/>
 </node>
 <node id="21441547" lat="50.9839461" lon="11.3309148">
  <tag k="barrier" v="bollard"/>
 </node>
</osm>
"""

    @Test
    fun testScannerFindsAllNodes() {
        val numOSMNodes = 2
        val expectedXMLNodes = numOSMNodes + 2 // +1 for osm, +1 for bounds
        var countedXMLNodes = 0
        object : XMLScanner(source.reader()) {
            override fun onStart(depth: Int, type: CharSequence): Boolean {
                println("Start($type@$depth)")
                countedXMLNodes++
                return when (type) {
                    "bounds", "osm" -> true
                    else -> false
                }
            }

            override fun onAttribute(depth: Int, type: CharSequence, key: CharSequence, value: CharSequence) {
                println("Attr($type@$depth).$key = $value")
            }

            override fun onContent(depth: Int, type: CharSequence, value: CharSequence) {
                println("Content($type@$depth): $value")
            }

            override fun onEnd(depth: Int, type: CharSequence) {
                println("End($type@$depth)")
            }
        }.scan()
        assertEquals(expectedXMLNodes, countedXMLNodes)
    }
}