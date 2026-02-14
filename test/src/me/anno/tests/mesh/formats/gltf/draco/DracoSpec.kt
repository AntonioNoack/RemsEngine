package me.anno.tests.mesh.gltf.draco

import me.anno.utils.OS
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.splitLines

/**
let codes = [...document.getElementsByTagName('code')]
codes.slice(65).map(x => x.innerText).join('\n\n\n')
 * */

fun main() {
    val functions = OS.desktop.getChild("draco-spec.txt")
        .readTextSync()
        .split("\n\n\n\n")
        .map { it.trim().splitLines() }
        .filter { it.isNotEmpty() }
    val prefix = "        eb                                                                            "
    for (func in functions) {
        val start = func[0].substring(func[0].indexOf2(' ') + 1)
        println("fun $start")
        for (i in 1 until func.size) {
            val line = func[i]
            if (line.length > prefix.length) {
                println(line.substring(0, prefix.length).trimEnd() + " = read" + line.substring(prefix.length).trim()+"()")
            } else {
                println(line)
            }
        }
        println()
    }
}