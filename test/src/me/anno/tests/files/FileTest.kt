package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import me.anno.utils.types.Strings
import org.apache.logging.log4j.LogManager

fun main() {

    // File("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8\\u8.zip!sve gemm/vm.zip").readText()

    val logger = LogManager.getLogger("FileTest")

    fun printHierarchy(file: FileReference, depth: Int) {
        logger.info(Strings.tabs(depth) + file.absolutePath)
        if (file.isSomeKindOfDirectory) {
            val children = file.listChildren()
            if (children.isNotEmpty()) {
                for (child in children)
                    printHierarchy(child, depth + 1)
            } else {
                logger.info(Strings.tabs(depth + 1) + "sadly empty...")
            }
        }
    }

    logger.info(getReference("res://meshes/arrowX.obj").readTextSync())

    // val file = getReference("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8")
    /*val zis = ZipInputStream(file.inputStream())
    while (true) {
        val entry = zis.nextEntry ?: break
        logger.info(entry.name)
    }

    logger.info(file.listFiles()?.joinToString())*/

    // printHierarchy(file, 0)

    /*val file2 = FileReference("E:\\Documents\\Uni\\Master\\SS21\\HPC\\u8\\u8.zip!!sve gemm/vm.zip")
    logger.info(file2.listFiles()?.joinToString())
    logger.info(file2.extension)
    logger.info("compressed: ${file2.isCompressed.value}")
    logger.info("directory: ${file2.isDirectory}")
    logger.info("insideCompressed: ${file2.isInsideCompressed}")*/
    // getReference(OS.desktop,file2.name).writeBytes(file2.inputStream().readBytes())

}

