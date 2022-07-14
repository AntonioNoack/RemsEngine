package me.anno.tests.ecs

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.io.ISaveable
import me.anno.io.files.InvalidRef
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter

fun main() {
    ISaveable.registerCustomClass(CAdd())
    ISaveable.registerCustomClass(CSet())
    ISaveable.registerCustomClass(Path.ROOT_PATH)
    val p0 = Path(Path.ROOT_PATH, "k", 4, 'x')
    val p1 = Path(p0, "l", 5, 'y')
    val path = Path(p1, "m", 6, 'z')
    for (sample in listOf(
        CSet(path, "path", "z"),
        CAdd(path, 'x', "Entity")
    )) {
        println(sample)
        val clone = TextReader.read(TextWriter.toText(sample, InvalidRef), InvalidRef, true).first()
        println(clone)
    }
}