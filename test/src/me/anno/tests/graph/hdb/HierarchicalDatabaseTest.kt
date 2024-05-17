package me.anno.tests.graph.hdb

import me.anno.graph.hdb.HDBKey
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.io.files.FileFileRef
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class HierarchicalDatabaseTest {

    @Test
    fun testDatabaseGettersAndSetters() {

        val folder = FileFileRef.createTempFile("db", "")
        folder.delete()
        folder.tryMkdirs()
        folder.deleteOnExit()

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
            "Test", folder,
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

        var ctr = 0
        for (i in samples.indices) {
            val name = "${sampleKeys[i].path}[$i]"
            instance.get(sampleKeys[i], false) { bytes ->
                if (bytes == null) {
                    fail("Missing $name")
                } else if (sampleData[i].contentEquals(bytes.getAsArray())) {
                    println("$name is fine")
                    ctr++
                } else {
                    fail("$name was saved incorrectly: ${bytes.getAsString()}")
                }
            }
        }
        assertEquals(samples.size, ctr)
    }
}

