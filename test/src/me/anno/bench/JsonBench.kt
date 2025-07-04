package me.anno.bench

// import com.fasterxml.jackson.databind.ObjectMapper
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.utils.Clock
import me.anno.utils.OS

fun main() {

    val file = OS.documents.getChild("RemsStudio\\Image Tests\\Scenes\\Benchmark.json")

    // Thread.sleep(10000)

    val tries = 20

    val timer = Clock("JsonBench")
    timer.start()

    val text = file.readTextSync()

    timer.stop("just reading")

    // warm-up
    // ObjectMapper().readTree(text)
    timer.start()

    /* for (i in 0 until tries) {
         ObjectMapper().readTree(text)
     }*/

    timer.stop("jackson")

    // can we make our library more efficient?
    // 992kB without spaces, 1500kB with them
    // jackson: 0.199s for  10 runs
    //          1.269s for 100 runs
    //          3.000s for 250 runs
    // custom:  0.612s for  10 runs
    //          3.439s for 100 runs
    //          7.699s for 250 runs

    // I would have expected the FileFileRef allocations to take time, but it looks like, they don't...

    // warm-up
    JsonStringReader.read(text, InvalidRef, false)
    timer.start()

    for (i in 0 until tries) {
        JsonStringReader.read(text, InvalidRef, false)
    }

    timer.stop("custom")

}