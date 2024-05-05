package me.anno.jvm.build.web

import me.anno.io.files.FileReference
import me.anno.utils.OS.documents

var ctr = 0
fun build(src: List<FileReference>, dst: FileReference, path: String) {
    if (src.first().isDirectory) {
        dst.tryMkdirs()
        val srcChildren = src.flatMap { it.listChildren() }.map { it.name }.toHashSet()
        for (srcI in srcChildren) {
            val srcII = src.map { it.getChild(srcI) }.filter { it.exists }
            build(srcII, dst.getChild(srcI), "$path/${srcI}")
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
                    .replace(".toSortedSet()", ".toSet()") // not supported
                    .replace("System.currentTimeMillis()", "kotlin.js.Date.now().toLong()")
                    .replace(".qualifiedName!!", ".simpleName!!") // not supported
                    .replace("StackOverflowError", "IllegalStateException")
                    .replace("::class.name", "::class.simpleName")
                    .replace("lengthI.binarySearch", "lengthI.asList().binarySearch")
                    .replace("times.binarySearch", "times.asList().binarySearch")
                    .replace("file is FileFileRef && !(file.file.run { canRead() && canWrite() })", "false")
                    .replace("this::class.classLoader.loadClass(", "java.lang.System.findClass(")
                    .replace(".superclasses.firstOrNull()", ".findParentClass()")
                    .replace("import kotlin.reflect.full.", "// import kotlin.reflect.full.")
                    .replace("import kotlin.reflect.jvm.simpleName", "// import kotlin.reflect.jvm.simpleName")
                    // arrays don't have type information in Kotlin/JS, but still need some to be constructed -> impractical for cloning
                    .replace("= Array<", "= createArrayList<")
                    .replace("(Array(", "(createArrayList(")
                    .replace("Array<", "ArrayList<")
                    .replace(" Array(", " createArrayList(")
                    .replace("arrayOfNulls", "createArrayListOfNulls")
                    .replace("arrayOf(", "arrayListOf(")
                    .replace("arrayOf<", "arrayListOf<")
                    .replace("emptyArray()", "arrayListOf()")
                    .replace(".toTypedArray()", ".toArrayList()")
                    .replace("emptyArrayList<Any>()", "arrayListOf<Any>()")
                    .replace("targets: ArrayList<TargetType>,", "targets: List<TargetType>,")
                    .replace(
                        "java.lang.reflect.Array.newInstance(array::class.componentType, size)",
                        "createArrayListOfNulls<Any>(size)"
                    )
                    .replace("ArrayList<Saveable?>(values.size) { values[it] }", "ArrayList<Saveable?>(values)")
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
                importIf("NoSuchMethodException", "java.lang.NoSuchMethodException")
                importIf("NoSuchFieldException", "java.lang.NoSuchFieldException")
                importIf("ThreadLocal", "java.lang.ThreadLocal")
                importIf(".findParentClass()", "java.lang.System.findParentClass")
                importIf(".newInstance()", "java.lang.System.newInstance")
                importIf(".getMethod(", "java.lang.System.getMethod")
                importIf(".getField(", "java.lang.System.getField")
                importIf(".type", "java.lang.System.type")
                importIf("createArrayList", "java.lang.System.createArrayList")
                importIf("createArrayListOfNulls", "java.lang.System.createArrayListOfNulls")
                importIf("toArrayList()", "java.lang.System.toArrayList")
                importIf("copyInto", "java.lang.System.copyInto")
                importIf("copyOfRange", "java.lang.System.copyOfRange")

                if (prevTxt != txt) {
                    dst.writeText(txt)
                }
            } else println("Ignored $dst")
        } else {
            srcI.copyTo(dst) {}
        }
    }
}

fun clear(src: List<FileReference>, dst: FileReference, path: String) {
    if (src.first().isDirectory) {
        dst.tryMkdirs()
        val srcChildren = src.flatMap { it.listChildren() }.map { it.name }.toHashSet()
        for (srcI in srcChildren) {
            val srcII = src.map { it.getChild(srcI) }.filter { it.exists }
            clear(srcII, dst.getChild(srcI), "$path/${srcI}")
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
                dst.delete()
            } else println("Ignored $dst")
        }// else ignored
    }
}

val projects = documents.getChild("IdeaProjects")
val src = projects.getChild("RemsEngine")
val srcI = listOf(
    src.getChild("src"),
    src.getChild("KOML/src")
)
val dst = projects.getChild("RemsEngineWeb")
val dstI = dst.getChild("src/jsMain/kotlin")

fun main() {
    if (false) {
        clear(srcI, dstI, "")
    } else {
        build(srcI, dstI, "")
    }
    println("$ctr kotlin files total")
}