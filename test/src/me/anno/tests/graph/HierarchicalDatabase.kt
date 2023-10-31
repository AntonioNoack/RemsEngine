package me.anno.tests.graph

import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.studio.StudioBase
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
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

    val timeout: Long = 1000 // Int.MAX_VALUE.toLong()
    val instance = HierarchicalDatabase("Test", desktop.getChild("db"), 10_000, timeout, timeout)
    instance.get(listOf(), 0L, false) {
        println("got key: ${it?.size}")

        if (true) {
            val sampleData = pictures.getChild("4k.jpg").readBytesSync()
            instance.put(listOf(), 0L, sampleData)
        }
        StudioBase.workEventTasks()
    }

}
