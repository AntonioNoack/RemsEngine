package me.anno.tests.utils

import me.anno.ecs.prefab.ChangeHistory
import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter


/**
 * a test for StringHistories compression capabilities
 * */
fun main() {

    Saveable.registerCustomClass(ChangeHistory())

    val hist = ChangeHistory()
    hist.put("hallo")
    hist.put("hello")
    hist.put("hello world")
    hist.put("hell")
    hist.put("hello world, you")
    hist.put("kiss the world")
    hist.put("this is the world")
    hist.put("that was le world")

    val str = JsonStringWriter.toText(hist, InvalidRef)
    println(str)

    val hist2 = JsonStringReader.readFirstOrNull<ChangeHistory>(str, InvalidRef)!!

    val str2 = JsonStringWriter.toText(hist2, InvalidRef)

    if (str != str2) {
        println(str2)
        throw RuntimeException()
    }

}