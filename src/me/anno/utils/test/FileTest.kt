package me.anno.utils.test

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.Tabs

fun main() {

    // File("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8\\u8.zip!sve gemm/vm.zip").readText()

    val file = getReference("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8")
    /*val zis = ZipInputStream(file.inputStream())
    while (true) {
        val entry = zis.nextEntry ?: break
        println(entry.name)
    }

    println(file.listFiles()?.joinToString())*/

    printHierarchy(file, 0)

    /*val file2 = FileReference("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8\\u8.zip!!sve gemm/vm.zip")
    println(file2.listFiles()?.joinToString())
    println(file2.extension)
    println("compressed: ${file2.isCompressed.value}")
    println("directory: ${file2.isDirectory}")
    println("insideCompressed: ${file2.isInsideCompressed}")*/
    // getReference(OS.desktop,file2.name).writeBytes(file2.inputStream().readBytes())

}

fun printHierarchy(file: FileReference, depth: Int) {
    println(Tabs.tabs(depth) + file.absolutePath)
    if (file.isSomeKindOfDirectory) {
        val children = file.listChildren() ?: emptyList()
        if (children.isNotEmpty()) {
            for (child in children)
                printHierarchy(child, depth + 1)
        } else {
            println(Tabs.tabs(depth + 1) + "sadly empty...")
        }
    }
}

