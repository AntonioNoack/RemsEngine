package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.utils.OS.home

// I just wanted to reformat a few cpp files
fun main() {
    format(home.getChild("source\\repos\\NvidiaRayTracingKHR2\\gl_vk_raytrace_interop"), 1)
}

fun format(file: FileReference, depth: Int) {
    if (file.isDirectory) {
        if (depth > 0) {
            for (child in file.listChildren() ?: return) {
                format(child, depth - 1)
            }
        }
    } else when (file.lcExtension) {
        "h", "c", "hpp", "c++" -> {
            file.readText { text, exception ->
                exception?.printStackTrace()
                if (text != null) {
                    file.writeText(format(text))
                }
            }
        }
    }
}

fun format(text: String): String {
    val builder = StringBuilder(text.length)
    val tabs = text.replace("  ", "\t")
    var i = 0
    while (true) {
        val j = tabs.indexOf('{', i)
        if (j < 0) break
        // find where the last stuff is, skip whitespace
        var k = j - 1 // first non-whitespace
        while (k >= 0 && tabs[k].isWhitespace()) {
            k--
        }
        // if endsWith "const" or ")", remove all those tabs
        val last = tabs.subSequence(0, k + 1)
        var l = k // first whitespace
        while (l >= 0 && tabs[l].isLetterOrDigit()) {
            l--
        }
        var m = l // first non-whitespace
        while (m >= 0 && tabs[m].isWhitespace()) {
            m--
        }
        val last2 = if (m < k) tabs.subSequence(0, m + 1) else ""
        if (tabs[k] == ')' ||
            last.endsWith("const") ||
            last.endsWith("struct") || last2.endsWith("struct") ||
            last.endsWith("class") || last2.endsWith("class") ||
            last.endsWith("override")
        ) {
            builder.append(tabs.subSequence(i, k + 1))
            builder.append(" {")
        } else {
            builder.append(tabs.subSequence(i, j + 1))
        }
        i = j + 1
    }
    builder.append(tabs.subSequence(i, tabs.length))
    return builder.toString()
}