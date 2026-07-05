package me.anno.experiments.kotlinMultiplatform

import java.io.File

fun main() {
    val projects = File("/mnt/Windows/Users/Antonio/Documents/IdeaProjects")
    val dst = File(projects, "RemsEngineKMP/sharedLogic/src/commonMain/kotlin")
    copyTo(File(projects, "RemsEngine/src"), dst)
    copyTo(File(projects, "RemsEngine/KOML/src"), dst)
}

private val syncRegex = Regex("synchronized\\([\\w.]+\\)")

private fun copyTo(src: File, dst: File) {
    if (src.isDirectory) {
        check(!dst.exists() || dst.isDirectory)
        dst.mkdir()
        for (child in src.listFiles()!!) {
            copyTo(child, File(dst, child.name))
        }
    } else {
        if (dst.exists() && dst.readText().contains("#custom")) return
        if (src.extension == "kt") {
            val content = src.readLines()
                .map {
                    it
                        .replace("::class.java", "::class")
                        .replace(".javaClass", "::class")
                        .replace(": Class<", ": KClass<")
                        .replace(syncRegex, "run")
                        .replace("@JvmStatic", "")
                        .replace("@JvmOverloads", "")
                        .replace("@JvmField", "")
                        .replace("clazz.java.newInstance()", "clazz.createInstance()")
                        .replace("override fun removeIf(", "fun removeIf(")
                }
                .joinToString("\n")
            dst.writeText(content)
        } else if (src.extension != "md") {
            src.copyTo(dst, overwrite = true)
        }
    }
}