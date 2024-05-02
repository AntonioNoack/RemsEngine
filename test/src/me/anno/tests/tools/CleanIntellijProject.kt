package me.anno.tests.tools

import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference
import org.apache.logging.log4j.LogManager

/**
 * cleans temporary files from projects that I wanted to archive
 * */
fun main() {

    val logger = LogManager.getLogger("CleanIntellijProject")

    fun deleteClassFiles(file: FileReference) {
        if (file.isDirectory) {
            for (f in file.listChildren()) {
                deleteClassFiles(f)
            }
        } else {
            if (file.extension.equals("class", true)) {
                file.delete()
            }
        }
    }

    fun cleanEclipseProject(file: FileReference) {

        if (!file.getChild(".classpath").exists) {
            logger.info("not a project: $file")
            return
        }

        file.getChild("bin").deleteRecursively()

    }

    fun deleteEmptyFolders(file: FileReference) {

        if (!file.isDirectory) return

        for (f in file.listChildren()) {
            deleteEmptyFolders(f)
        }

        if (file.listChildren().isEmpty()) {
            logger.info("deleting $file")
            file.deleteRecursively()
        }

    }

    fun cleanIntellijProject(file: FileReference) {

        if (!file.isDirectory) return

        if (file.listChildren().none { it.extension == "iml" }) {
            return
        }

        logger.info(file.name)

        val files = listOf(".gradle", "gradle", "out", "build", "app/build", "captures", "app/.externalNativeBuild")

        for (f in files) {
            file.getChild(f).deleteRecursively()
        }

        val release = file.getChild("app/release")
        if (release.exists) {
            if (release.listChildren().size > 2) {
                logger.info("Problematic: $file")
            } else {
                release.deleteRecursively()
            }
        }

        deleteEmptyFolders(file)

    }

    val folder = getReference("E:\\Projects\\Android")
    for (file in folder.listChildren()) {
        cleanIntellijProject(file)
    }

    val folder2 = getReference("E:\\Projects\\Java")
    for (file in folder2.listChildren()) {
        cleanEclipseProject(file)
        cleanIntellijProject(file)
        deleteClassFiles(file)
        deleteEmptyFolders(file)
    }

}
