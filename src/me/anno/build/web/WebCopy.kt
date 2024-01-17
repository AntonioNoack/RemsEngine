package me.anno.build.web

import me.anno.io.files.FileReference
import me.anno.utils.OS.documents

fun copy(src: List<FileReference>, dst: FileReference, path: String) {
    if (src.first().isDirectory) {
        dst.tryMkdirs()
        val srcChildren = src.map { it.listChildren()!! }.flatten().map { it.name }.toHashSet()
        for (srcI in srcChildren) {
            val srcII = src.map { it.getChild(srcI) }.filter { it.exists }
            copy(srcII, dst.getChild(srcI), "$path/${srcI}")
        }
        val dstChildren = dst.listChildren()!!
        val dstNames = dstChildren.map { it.name }.filter { it !in srcChildren }
        if (dstNames.isNotEmpty()) {
            println("Foreign files in $dst: $dstNames")
        }
    } else {
        val srcI = src.first()
        if (srcI.lcExtension == "kt") {
            val prevTxt = if (dst.exists) dst.readTextSync() else ""
            if (!prevTxt.contains("!ignore")) {
                var txt = srcI.readTextSync()
                    .replace("@JvmStatic", "/* @JvmStatic */")
                    .replace("@JvmField", "/* @JvmField */")
                    .replace("@Throws", "// @Throws")
                    .replace("@JvmOverloads", "/* @JvmOverloads */")
                    .replace("synchronized(", "sync1(")
                    .replace(
                        "override fun removeIf(p0: Predicate<in V>): Boolean {",
                        "/*override*/ fun removeIf(p0: Predicate<in V>): Boolean {"
                    )
                    .replace("OutOfMemoryError", "java.lang.OutOfMemoryError")
                    .replace("kotlin.concurrent", "kotlini.concurrent")
                    .replace("kotlin.test", "kotlini.test")
                    .replace("class.qualifiedName", "class.simpleName") // not supported :/
                    .replace(".codePoints()", ".map { it.code }") // not supported :/
                    .replace("Character.toChars(this)", "charArrayOf(toChar())")
                    .replace("import kotlin.streams.toList", "// import kotlin.streams.toList")
                    .replace("GL11C", "OpenGL")
                    .replace("GL15C", "OpenGL")
                    .replace("GL20C", "OpenGL")
                    .replace("GL21C", "OpenGL")
                    .replace("GL30C", "OpenGL")
                    .replace("GL31C", "OpenGL")
                    .replace("GL32C", "OpenGL")
                    .replace("GL33C", "OpenGL")
                    .replace("GL40C", "OpenGL")
                    .replace("GL42C", "OpenGL")
                    .replace("GL43C", "OpenGL")
                    .replace("GL45C", "OpenGL")
                    .replace("GL46C", "OpenGL")
                if ("sync1(" in txt) {
                    val i = txt.indexOf('\n')
                    txt = txt.substring(0, i + 1) + "import sync1" + txt.substring(i)
                }
                if ("Thread." in txt) {
                    val i = txt.indexOf('\n')
                    txt = txt.substring(0, i + 1) + "import kotlini.concurrent.Thread" + txt.substring(i)
                }
                if (prevTxt != txt) {
                    dst.writeText(txt)
                }
            } else println("Ignored $dst")
        } else {
            srcI.copyTo(dst) {}
        }
    }
}

fun main() {
    val projects = documents.getChild("IdeaProjects")
    val src = projects.getChild("RemsEngine")
    val dst = projects.getChild("RemsEngineWeb")
    copy(
        listOf(
            src.getChild("src"),
            src.getChild("KOML/src")
        ), dst.getChild("src/jsMain/kotlin"), ""
    )
}