package me.anno.utils.test

import me.anno.io.files.FileReference
import me.anno.utils.OS.documents
import me.anno.utils.types.Strings.addPrefix

fun main() {

    val graph = HashMap<String, HashSet<String>>()
    val source = documents.getChild("IdeaProjects/VideoStudio/src")

    val ignoredPaths = listOf(
        "kotlin.",
        "java.",
        "javax."
    )

    // val mainPath = "me.anno."

    // create dependency graph between folders and files
    fun traverse(folder: FileReference, folderPath: String?) {
        for (file in folder.listChildren() ?: return) {
            val filePath = addPrefix(folderPath, "/", file.name)
            if (file.isDirectory) {
                traverse(file, filePath)
            } else {
                when (file.lcExtension) {
                    "java", "kt" -> {
                        val lines = file.readText().split('\n')
                            .filter { line ->
                                val startIndex = line.indexOfFirst { c -> c != ' ' && c != '\t' }
                                startIndex >= 0 && (line.startsWith("import ", startIndex) || line.startsWith(
                                    "import\t",
                                    startIndex
                                ))
                            }
                        if (lines.isNotEmpty()) {
                            val name = folderPath ?: ""
                            val node = graph.getOrPut(name) { HashSet() }
                            for (line in lines) {
                                val startIndex0 = line.indexOfFirst { c -> c != ' ' && c != '\t' } + "import".length
                                val startIndex1 = line.withIndex()
                                    .indexOfFirst { (index, c) -> index > startIndex0 && c != ' ' && c != '\t' }
                                val endIndex = line.withIndex().indexOfFirst { (index, c) ->
                                    index > startIndex1 && c !in 'A'..'Z' && c !in 'a'..'z' && c !in '0'..'9' &&
                                            c !in ".*"
                                }
                                if (startIndex1 in (startIndex0 + 1) until endIndex) {
                                    val importPath = line.substring(startIndex1, endIndex)
                                    if (ignoredPaths.none { importPath.startsWith(it) }) {
                                        /*if (importPath.startsWith(mainPath)) {
                                            importPath = importPath.substring(mainPath.length)
                                        }*/
                                        node.add(importPath)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    traverse(source, null)

    // todo visualize graph?
    // todo find parts, which could be extracted

    for ((name, node) in graph.toSortedMap()) {
        // todo merge similar paths into stars
        println("$name: ${node.sorted()}")
    }

}