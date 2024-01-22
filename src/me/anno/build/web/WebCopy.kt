package me.anno.build.web

import me.anno.io.files.FileReference
import me.anno.utils.OS.documents

var ctr = 0
fun copy(src: List<FileReference>, dst: FileReference, path: String) {
    if (src.first().isDirectory) {
        dst.tryMkdirs()
        val srcChildren = src.map { it.listChildren() }.flatten().map { it.name }.toHashSet()
        for (srcI in srcChildren) {
            val srcII = src.map { it.getChild(srcI) }.filter { it.exists }
            copy(srcII, dst.getChild(srcI), "$path/${srcI}")
        }
        val dstChildren = dst.listChildren()
        val dstNames = dstChildren.map { it.name }.filter { it !in srcChildren }
        if (dstNames.isNotEmpty()) {
            System.err.println("Foreign files in $dst: $dstNames")
        }
    } else {
        val srcI = src.first()
        if (srcI.lcExtension == "kt") {
            ctr++
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
                    .replace("Character.toChars(this)", "charArrayOf(toChar())")
                    .replace("import kotlin.streams.toList", "// import kotlin.streams.toList")
                    .replace(".javaClass", "::class")
                    .replace("InterruptedException", "java.lang.InterruptedException")
                    .replace(" Class<", " KClass<")
                    .replace(".jvmName", ".simpleName")
                    .replace("clazz.java.", "clazz.")
                    .replace("System.err.println", "console.error")
                    .replace("javaClass", "this::class")
                    .replace("::class.java", "::class")
                    .replace("System.gc()", "// System.gc()")
                    .replace("times.binarySearch", "times.asList().binarySearch")
                    .replace("content.binarySearch(", "content.asList().binarySearch(")
                    .replace(".toSortedSet()", ".toSet()") // not supported
                    .replace("System.currentTimeMillis()", "kotlin.js.Date.now().toLong()")
                    .replace(".qualifiedName!!", ".simpleName!!") // not supported
                    .replace("StackOverflowError", "IllegalStateException")
                    .replace("::class.name", "::class.simpleName")
                    .replace("file is FileFileRef && !(file.file.run { canRead() && canWrite() })", "false")
                    .replace("this::class.classLoader.loadClass(", "System.findClass(")
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
                    .replace("GL14", "OpenGL")

                fun importIf(condition: String, importName: String) {
                    if (condition !in txt) return
                    val statement = "import $importName"
                    if (statement in txt) return
                    val i = txt.indexOf('\n')
                    txt = txt.substring(0, i + 1) + statement + txt.substring(i)
                }

                importIf("sync1(", "sync1")
                importIf("Thread.", "kotlini.concurrent.Thread")
                importIf(".format(", "java.lang.System.format")
                importIf("KClass", "kotlin.reflect.KClass")
                importIf("Runtime.getRuntime()", "java.lang.Runtime")
                importIf("System.", "java.lang.System")
                importIf(".nextDown()", "java.lang.System.nextDown")
                importIf(".toSortedMap()", "java.lang.System.toSortedMap")
                importIf("NoClassDefFoundError", "java.lang.NoClassDefFoundError")
                importIf("ClassNotFoundException", "java.lang.ClassNotFoundException")
                importIf("InstantiationException", "java.lang.InstantiationException")
                importIf("ThreadLocal", "java.lang.ThreadLocal")

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
    println("$ctr kotlin files total")
}