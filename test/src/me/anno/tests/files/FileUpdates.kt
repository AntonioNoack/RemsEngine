package me.anno.tests.files

import me.anno.Engine
import me.anno.engine.RemsEngine
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS.documents
import me.anno.utils.Sleep.waitUntil
import kotlin.concurrent.thread

fun main() {

    // goal: the meshes in the engine, and the file explorer need to update
    // does this work for imported meshes? yes :)

    val folder = documents
    val file1 = getReference(folder, "t1.obj")
    val file2 = getReference(folder, "t2.obj")

    thread(name = "ToggleThread") {
        var ctr = 0
        var data1: ByteArray? = null
        getReference(documents, "cube.obj").readBytes { it, exc ->
            data1 = if (it == null) {
                exc!!.printStackTrace()
                ByteArray(0)
            } else it
        }
        var data2: ByteArray? = null
        getReference(documents, "sphere.obj").readBytes { it, exc ->
            data2 = if (it == null) {
                exc!!.printStackTrace()
                ByteArray(0)
            } else it
        }
        waitUntil(true) { data1 != null && data2 != null }
        Thread.sleep(5000)
        var toggle = false
        while (!Engine.shutdown && ctr < 100) {
            Thread.sleep(200)
            if (ctr++ % 10 == 0) {
                // switch the contents of file1 and file2 every 2 seconds
                if (toggle) {
                    file1.writeBytes(data1!!)
                    file2.writeBytes(data2!!)
                } else {
                    file2.writeBytes(data1!!)
                    file1.writeBytes(data2!!)
                }
                toggle = !toggle
            }
        }
    }

    RemsEngine().run()

}