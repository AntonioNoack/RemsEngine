package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.tests.LOGGER
import me.anno.utils.OS.documents
import me.anno.utils.structures.Iterators.count
import me.anno.utils.types.Strings.isBlank2

fun main() {
    val project = documents.getChild("IdeaProjects/RemsEngine")
    LOGGER.info("src: ${countLines(project.getChild("src/me/anno"))}")
    LOGGER.info("Box2D: ${countLines(project.getChild("Box2d"))}")
    LOGGER.info("Image: ${countLines(project.getChild("Image"))}")
    LOGGER.info("JVM: ${countLines(project.getChild("JVM"))}")
    LOGGER.info("PDF: ${countLines(project.getChild("PDF"))}")
    LOGGER.info("Mesh: ${countLines(project.getChild("Mesh"))}")
    LOGGER.info("Recast: ${countLines(project.getChild("Recast"))}")
    LOGGER.info("SDF: ${countLines(project.getChild("SDF"))}")
    LOGGER.info("Lua: ${countLines(project.getChild("Lua"))}")
    LOGGER.info("KOML: ${countLines(project.getChild("KOML"))}")
    LOGGER.info("Unpack: ${countLines(project.getChild("Unpack"))}")
    LOGGER.info("Video: ${countLines(project.getChild("Video"))}")
    LOGGER.info("Bullet: ${countLines(project.getChild("Bullet"))}")
    LOGGER.info("BulletJME: ${countLines(project.getChild("BulletJME"))}")
    LOGGER.info("Test: ${countLines(project.getChild("test/src"))}")
}

fun countLines(file: FileReference): Int {
    return if (file.isDirectory) {
        file.listChildren().sumOf { countLines(it) }
    } else when (file.lcExtension) {
        "kt", "java" -> {
            file.readLinesSync(64).count {
                !it.isBlank2() && !it.trim().startsWith("//")
            }
        }
        else -> 0
    }
}