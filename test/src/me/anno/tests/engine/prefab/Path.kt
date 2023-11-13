package me.anno.tests.engine.prefab

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter

fun main() {

    val p123 = Path(arrayOf("1", "2", "3"), intArrayOf(1, 2, 3), charArrayOf('a', 'b', 'c'))
    if (p123.size != 3) throw RuntimeException()

    val p12 = p123.parent!!
    val p1 = p12.parent!!
    val p0 = p1.parent!!

    if (p0 !== Path.ROOT_PATH) throw RuntimeException()
    if (p0 == p1) throw RuntimeException()
    if (p0 == p12) throw RuntimeException()
    if (p0 == p123) throw RuntimeException()
    if (p1 == p12) throw RuntimeException()
    if (p1 == p123) throw RuntimeException()
    if (p12 == p123) throw RuntimeException()

    val p12123 = Path.join(p12, p123)
    if (p12123.toString() != "a1,1/b2,2/a1,1/b2,2/c3,3") throw RuntimeException()

    if (p0.toString() == p1.toString()) throw RuntimeException()
    if (p0.toString() == p12.toString()) throw RuntimeException()
    if (p0.toString() == p123.toString()) throw RuntimeException()
    if (p1.toString() == p12.toString()) throw RuntimeException()
    if (p1.toString() == p123.toString()) throw RuntimeException()
    if (p12.toString() == p123.toString()) throw RuntimeException()

    val groundTruth = p123.toString()
    val copy = Path.parse(groundTruth)
    val copied = copy.toString()
    val matchSerialized = groundTruth == copied
    val matchInstance = p123 == copy
    println("$matchSerialized, $matchInstance, $groundTruth vs $copied")
    if (!matchInstance || !matchSerialized) throw RuntimeException()

    val abc = Path(arrayOf("a", "b", "c"), intArrayOf(0, 1, 2), charArrayOf('x', 'x', 'x'))
    val bcd = Path(arrayOf("b", "c", "d"), intArrayOf(1, 2, 3), charArrayOf('x', 'x', 'x'))

    println("abc x abc, 0: '${abc.getSubPathIfMatching(abc, 0)}'")
    println("abc x abc, 1: '${abc.getSubPathIfMatching(abc, 1)}'")
    println("abc x bcd, 1: '${abc.getSubPathIfMatching(bcd, 1)}'")

    ECSRegistry.init()
    println(p123)
    val cloned = JsonStringReader.read(JsonStringWriter.toText(p123, InvalidRef), InvalidRef, false).first()
    println(cloned)
    println(cloned == p123)

    val prefab = Prefab("Entity")
    val sample = prefab.getSampleInstance()
    if (sample.prefabPath != Path.ROOT_PATH) throw RuntimeException()
    val c1 = prefab.add(Path.ROOT_PATH, 'e', "Entity", "C1")
    /*val c2 = */prefab.add(c1, 'e', "Entity", "C2")
    // val c3 = prefab.add(c2, 'e', "Entity", "C3")

    val adds = prefab.adds

    for (i in adds.indices) {
        val x0 = JsonStringWriter.toText(adds[i], InvalidRef)
        val x1 = JsonStringReader.read(x0, InvalidRef, false)[0] as CAdd
        val x2 = JsonStringWriter.toText(x1, InvalidRef)
        if (x0 != x2) {
            println(JsonFormatter.format(x0))
            println(JsonFormatter.format(x2))
            throw RuntimeException()
        }
    }

    val json = JsonStringWriter.toText(prefab, InvalidRef)
    val prefabClone = JsonStringReader.read(json, InvalidRef, false)[0] as Prefab

    println(prefab.adds)

    println(JsonFormatter.format(json))

    println(prefabClone.adds)
    val json2 = JsonStringWriter.toText(prefabClone, InvalidRef)
    println(JsonFormatter.format(json2))
    if (json != json2) throw RuntimeException()


}