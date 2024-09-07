package me.anno.tests.map

import me.anno.tests.map.OSMReaderV1.readOSM1
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.OS.downloads

fun main() {

    // load OSM data, and visualize it in real time
    // https://www.openstreetmap.org/export
    // might be useful to somebody...

    downloads.getChild("map2.osm")
        .readBytes { data, err ->
            err?.printStackTrace()
            if (data != null) {
                val tags = false
                /*val clock = Clock()
                clock.benchmark(5, 20, "map1") {
                    readOSM1(data.inputStream(), tags)
                }
                // 2.3x faster :)
                clock.benchmark(5, 20, "map2") {
                    readOSM2(data.inputStream(), tags)
                }*/

                testUI3("OSMap") {
                    OSMPanelV1(readOSM1(data.inputStream(), tags))
                }
            }
        }
}