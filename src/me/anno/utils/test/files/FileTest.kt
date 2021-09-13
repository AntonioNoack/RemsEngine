package me.anno.utils.test.files

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.LOGGER
import me.anno.utils.Tabs

fun main() {

    // File("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8\\u8.zip!sve gemm/vm.zip").readText()

    val file = getReference("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8")
    /*val zis = ZipInputStream(file.inputStream())
    while (true) {
        val entry = zis.nextEntry ?: break
        LOGGER.info(entry.name)
    }

    LOGGER.info(file.listFiles()?.joinToString())*/

    printHierarchy(file, 0)

    /*val file2 = FileReference("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8\\u8.zip!!sve gemm/vm.zip")
    LOGGER.info(file2.listFiles()?.joinToString())
    LOGGER.info(file2.extension)
    LOGGER.info("compressed: ${file2.isCompressed.value}")
    LOGGER.info("directory: ${file2.isDirectory}")
    LOGGER.info("insideCompressed: ${file2.isInsideCompressed}")*/
    // getReference(OS.desktop,file2.name).writeBytes(file2.inputStream().readBytes())

}

fun printHierarchy(file: FileReference, depth: Int) {
    LOGGER.info(Tabs.tabs(depth) + file.absolutePath)
    if (file.isSomeKindOfDirectory) {
        val children = file.listChildren() ?: emptyList()
        if (children.isNotEmpty()) {
            for (child in children)
                printHierarchy(child, depth + 1)
        } else {
            LOGGER.info(Tabs.tabs(depth + 1) + "sadly empty...")
        }
    }
}

