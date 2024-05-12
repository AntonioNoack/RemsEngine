package me.anno.tests.graph.hdb

import me.anno.graph.hdb.HDBKey
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.maths.Maths
import me.anno.utils.OS.desktop

fun main() {

    val runs = 500
    val file = desktop.getChild("tmp")
    file.deleteRecursively()

    val added = HashSet<HDBKey>()

    val db = HierarchicalDatabase("test", file, runs * 20, 1000, 0)

    fun add(key: HDBKey) {
        db.put(key, ByteArray(20) { (Maths.random() * 255).toInt().toByte() })
    }

    fun deleteRun() {
        val count = (Maths.random() * 10).toInt()
        for (i in 0 until count) {
            if (added.isEmpty()) break
            db.remove(added.random())
        }
    }

    fun addRun() {
        val count = (Maths.random() * 20).toInt()
        for (i in 0 until count) {
            if (Maths.random() < 0.3 || added.isEmpty()) {
                // add a new folder
                val folder = (added.randomOrNull()?.path ?: emptyList()) + "${Maths.random()}"
                add(HDBKey(folder, (Maths.random() * 1e16).toLong()))
            } else {
                // use an existing folder
                val folder = added.random().path
                add(HDBKey(folder, (Maths.random() * 1e16).toLong()))
            }
        }
    }

    for (i in 0 until runs) {
        deleteRun()
        addRun()
    }
}
