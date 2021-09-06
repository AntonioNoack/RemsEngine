package me.anno.utils.test

import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.objects.Transform
import me.anno.utils.Clock
import java.io.File

fun <V> measure(name: String, func: () -> V): V {
    val c = Clock()
    val value = func()
    c.stop(name)
    return value
}

fun main() {
   for(i in 0 until 100){
       test()
   }
}

fun test(){
    // ratios aren't too great, but not too bad either
    val lines = 1_000_000
    val genName = { Math.random().toString() }
    // val readerFile = File.createTempFile("RemsStudio", ".tmp")
    val readerText = measure("Reader-create"){
        TextWriter().apply {
            writeStringArray("strings", (0 until lines).map { genName() }.toTypedArray())
        }.data.toString()
    }
    // readerFile.writeText(readerText)
    // LOGGER.info(readerText)
    // val nativeFile = File.createTempFile("RemsStudio", ".tmp")
    val nativeText = (0 until lines).joinToString("\n") { genName() }
    // nativeFile.writeText(nativeText)
    /*measure("Reader") {//   423 ms for 1M
        TextReader(readerFile.readText()).readProperty(Transform())
    }*/
    measure("Reader-raw") {//   415 ms for 1M
        TextReader(readerText).readProperty(Transform())
    }
    /*measure("Native-1") {// 119 ms for 1M
        nativeFile.readLines()
    }*/
    /*measure("Native-2") {// 106 ms for 1M
        nativeFile.readText().split('\n')
    }*/
    measure("Native-2-raw") {// 52 ms for 1M
        nativeText.split('\n')
    }
    // readerFile.delete()
    // nativeFile.delete()
}