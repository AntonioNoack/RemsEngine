package me.anno.tests.engine.prefab

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.io.Saveable
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter

fun main() {
    Saveable.registerCustomClass(CAdd())
    Saveable.registerCustomClass(CSet())
    Saveable.registerCustomClass(Path.ROOT_PATH)
    val p0 = Path(Path.ROOT_PATH, "k", 4, 'x')
    val p1 = Path(p0, "l", 5, 'y')
    val path = Path(p1, "m", 6, 'z')
    for (sample in listOf(
        CSet(path, "path", "z"),
        CAdd(path, 'x', "Entity")
    )) {
        println(sample)
        val clone = JsonStringReader.read(JsonStringWriter.toText(sample, InvalidRef), InvalidRef, true).first()
        println(clone)
    }
}