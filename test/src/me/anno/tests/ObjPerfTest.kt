package me.anno.tests

import me.anno.io.files.InvalidRef
import me.anno.mesh.obj.OBJMTLReader
import me.anno.mesh.obj.OBJReader
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.files.Files.formatFileSize
import java.io.IOException
import java.io.InputStream
import kotlin.random.Random

fun main() {

    val clock = Clock()

    // 1.75ns/byte (6clk), 10ns/int (34clk) (Ryzen 5 2600) -> pretty good for Java :)
    // C seems only 2x faster (by a stackoverflow post, where DarthGizka claims 115M ints/s at length 9-10 = 1GB/s)
    // https://stackoverflow.com/a/26939263/4979303, DarthGizka
    val ints0 = IntArray(65536) { it }.apply { shuffle(Random(1234)) }
    val ints1 = ints0.joinToString(" ", "", "x")
    val ints2 = ints1.toByteArray()
    var idx = 0
    val reader = OBJMTLReader(object : InputStream() {
        override fun read() = ints2[idx++].toInt()
    })
    clock.benchmark(
        50, 1000, ints2.size,
        "loading ${ints0.size} ints, ${ints2.size} bytes"
    ) {
        idx = 0
        reader.putBack = -1
        for (j in ints0.indices) {
            val read = reader.readInt()
            if (ints0[j] != read) throw IOException("Exception at $j: ${ints0[j]} != $read")
            reader.skipSpaces()
        }
    }

    // large OBJ file, we get so 5-7ns/byte
    val src = downloads.getChild("ogldev-source/crytek_sponza/sponza.obj")
    val data = src.readBytes()
    clock.benchmark(10, 100, data.size, "loading ${data.size.toLong().formatFileSize()}") {
        OBJReader(data.inputStream(), InvalidRef)
    }

}