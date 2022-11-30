package me.anno.tests

import me.anno.io.files.FileReference
import me.anno.utils.OS.documents
import me.anno.utils.structures.Iterators.filter
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
                        val lines = file.readLinesSync(Int.MAX_VALUE)
                            .filter { line ->
                                val startIndex = line.indexOfFirst { c -> c != ' ' && c != '\t' }
                                startIndex >= 0 && (line.startsWith("import ", startIndex) || line.startsWith(
                                    "import\t",
                                    startIndex
                                ))
                            }
                        if (lines.hasNext()) {
                            val name = folderPath ?: ""
                            val node = graph.getOrPut(name) { HashSet() }
                            for (line in lines) {
                                val startIndex0 = line.indexOfFirst { c -> c != ' ' && c != '\t' } + "import".length
                                val startIndex1 = line.withIndex()
                                    .indexOfFirst { (index, c) -> index > startIndex0 && c != ' ' && c != '\t' }
                                val endIndex1 = line.withIndex().indexOfFirst { (index, c) ->
                                    index > startIndex1 && c !in 'A'..'Z' && c !in 'a'..'z' && c !in '0'..'9' &&
                                            c !in ".*"
                                }
                                if (startIndex1 in (startIndex0 + 1) until endIndex1) {
                                    var importPath = line.substring(startIndex1, endIndex1)
                                    if (importPath == "static") {
                                        val startIndex2 = line.withIndex()
                                            .indexOfFirst { (index, c) -> index > endIndex1 && c != ' ' && c != '\t' }
                                        val endIndex2 = line.withIndex().indexOfFirst { (index, c) ->
                                            index > startIndex2 && c !in 'A'..'Z' && c !in 'a'..'z' && c !in '0'..'9' && c !in ".*"
                                        }
                                        importPath = line.substring(startIndex2, endIndex2)
                                    }
                                    if (ignoredPaths.none { importPath.startsWith(it) }) {
                                        /*if (importPath.startsWith(mainPath)) {
                                            importPath = importPath.substring(mainPath.length)
                                        }*/
                                        node.add(importPath)
                                    }
                                }
                            }
                        }
                        // lines.close()
                    }
                }
            }
        }
    }

    traverse(source, null)

    // todo visualize graph?
    // todo find parts, which could be extracted

    for ((nodeName, node) in graph.toSortedMap()) {
        // todo merge similar paths into [Name1,Name2,Name3]
        val byPrefix = HashMap<String, HashSet<String>>()
        node.filter { !it.startsWith(nodeName) }
            .forEach { pathName ->
                val index = pathName.lastIndexOf('.')
                if (index > 0) {
                    val name = pathName.substring(index + 1)
                    val path = pathName.substring(0, index)
                    byPrefix.getOrPut(path) { HashSet() }
                        .add(name)
                }
            }
        println(
            "$nodeName: [${
                byPrefix.toSortedMap().entries.joinToString { (path, names) ->
                    if (names.size == 1) "$path.${names.first()}" else "$path.${names.toSortedSet().joinToString("/")}"
                }
            }]"
        )
    }

}