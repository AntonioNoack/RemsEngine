package me.anno.tests.graph

import me.anno.Engine
import me.anno.graph.hdb.HDBKey
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.io.Streams.readText
import me.anno.utils.LOGGER
import me.anno.utils.OS.desktop
import org.apache.logging.log4j.LogManager

fun main() {

    // project goal:
    //  folder-based key-value storage,
    //   - keys are 64-bit hashes
    //   - values are small files, ~30kB
    //   - total count is ~10-100k files

    // index says, which files are where
    //  - each file may contain at max 50MB of files, or one folder

    // operations:
    //  - get() -> loads folder & keeps it in memory for a bit
    //  - set() -> get() + writes value after a small delay
    //  - shutdown()
    //  - init()
    //  - save()?
    //  - clean() -> deletes no longer used files (1-2 weeks)

    LogManager.logAll()

    val timeout: Long = Int.MAX_VALUE.toLong()
    val instance = HierarchicalDatabase(
        "Test", desktop.getChild("db"),
        100_000, timeout, timeout, "txt"
    )

    // instance.clear()

    val samples = ("Ethan,Olivia,Jackson,Sophia,Liam")
            .split(',')

    val sampleKeys = samples.map { sample ->
        HDBKey(listOf(sample), sample.hashCode().toLong())
    }

    val sampleData = samples.map {
        it.toByteArray()
    }

    for (i in samples.indices) {
        val data = sampleData[i]
        instance.put(sampleKeys[i], data)
    }

    for (i in samples.indices) {
        val name = "${sampleKeys[i].path}[$i]"
        instance.get(sampleKeys[i], false) { bytes ->
            if (bytes == null) {
                println("Missing $name")
            } else if (sampleData[i].contentEquals(bytes.stream().readBytes())) {
                println("$name is fine")
            } else {
                println("$name was saved incorrectly: ${bytes.stream().readText()}")
            }
        }
    }

    Engine.requestShutdown()
}
