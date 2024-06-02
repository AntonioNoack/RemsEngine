package me.anno.tests.mesh.obj

import me.anno.engine.OfficialExtensions
import me.anno.mesh.obj.OBJReader
import me.anno.utils.Clock
import me.anno.utils.OS
import org.apache.logging.log4j.LogManager

fun main() {

    OfficialExtensions.initForTests()

    val logger = LogManager.getLogger("ObjTest")
    val source = OS.downloads.getChild("San_Miguel/san-miguel.obj")
    // 20MB, so larger than the L3 cache of my CPU
    // so the theoretical speed limit is my memory bandwidth
    // 3.2Gb/s -> 400MB/s -> 20MB file should be readable within 0.05s
    if (source.length() < 100e6) {
        source.readText { it, _ ->
            val data = it!! // remove material references for clearer reading performance
                .replace("mtllib", "#mtllib")
                .toByteArray()
            val clock = Clock(logger)
            for (i in 0 until 1000) {
                clock.start()
                OBJReader(data.inputStream(), source)
                clock.stop("Reading OBJ with 20MB", data.size)
            }
        }
    } else {
        // removed, because now it's more complicated with the async api
        /*val clock = Clock()
        for (i in 0 until 1000) {
            clock.start()
            OBJReader(source.inputStream(), source)
            clock.stop("Reading OBJ with 20MB", source.length().toInt())
        }*/
    }
    // 0.5s, so 10x slower than possible... ok, but slow...
    // goes down to 0.13s after the first 10 runs
    // and with "optimizations" (trial and error what is faster),
    // we now get down to 0.105s
    // 2x slower than theoretically possible -> ok, I think
}