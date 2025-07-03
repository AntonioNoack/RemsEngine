package me.anno.tests.graph.hdb

import me.anno.engine.OfficialExtensions
import me.anno.graph.hdb.HDBKey
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.io.config.ConfigBasics
import me.anno.tests.FlakyTest
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNull
import me.anno.utils.types.size
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class HierarchicalDatabaseTest {

    val samples = ("Ethan,Olivia,Jackson,Sophia,Liam,Nathan,Galileo,Armstrong")
        .split(',')

    val sampleKeys = samples.map { sample ->
        HDBKey(listOf(sample), sample.hashCode().toLong())
    }

    val sampleData = samples.map {
        it.encodeToByteArray()
    }

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    @Test
    @FlakyTest
    @Execution(ExecutionMode.SAME_THREAD)
    fun testDatabaseGettersAndSetters() {

        val folder = ConfigBasics.configFolder.getChild("DB-Test")

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

        val split = samples.size / 2
        val firstRange = 0 until split
        val secondRange = split until samples.size

        instance.clear()
        verifyContentsEmpty(instance)
        testPut(instance, firstRange)
        verifyContents(instance, firstRange)

        instance.storeIndex()
        instance.clearMemory()
        verifyContents(instance, firstRange)
        testPut(instance, secondRange)
        verifyContents(instance, samples.indices)

        instance.clear()
        verifyContentsEmpty(instance)
    }

    private fun testPut(instance: HierarchicalDatabase, indices: IntRange) {
        val doneCounter = AtomicInteger(indices.size)
        indices.map { i ->
            thread {
                val data = sampleData[i]
                instance.put(sampleKeys[i], data) { _, _ ->
                    doneCounter.decrementAndGet()
                }
            }
        }
        // wait until everything is saved
        Sleep.waitUntil(true) { doneCounter.get() == 0 }
    }

    private fun verifyContents(instance: HierarchicalDatabase, indices: IntRange) {
        var ctr = 0
        val doneCtr = AtomicInteger(indices.size)
        for (i in indices) {
            instance.get(sampleKeys[i]) { bytes, err ->
                val name = "${sampleKeys[i].path}[$i]"
                err?.printStackTrace()
                if (bytes == null) {
                    doneCtr.decrementAndGet()
                    fail("Missing $name")
                } else if (sampleData[i].contentEquals(bytes.getAsArray())) {
                    println("$name is fine")
                    ctr++
                    doneCtr.decrementAndGet()
                } else {
                    doneCtr.decrementAndGet()
                    fail("$name was saved incorrectly: ${bytes.getAsString()}")
                }
            }
        }
        Sleep.waitUntil(true) { doneCtr.get() == 0 }
        assertEquals(indices.size, ctr)
    }

    private fun verifyContentsEmpty(instance: HierarchicalDatabase) {
        val doneCtr = AtomicInteger(samples.size)
        for (key in sampleKeys) {
            instance.get(key) { bytes, _ ->
                doneCtr.decrementAndGet()
                assertNull(bytes)
            }
        }
        Sleep.waitUntil(true) { doneCtr.get() == 0 }
    }
}

