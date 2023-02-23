package me.anno.tests

import me.anno.io.files.FileReference
import me.anno.utils.OS.documents
import me.anno.utils.structures.Iterators.filter
import me.anno.utils.structures.Iterators.map
import me.anno.utils.types.Strings.addPrefix

fun main() {

    val graph = HashMap<String, HashSet<String>>()
    val source = documents.getChild("IdeaProjects/VideoStudio/src")

    val ignoredPaths = listOf(
        "kotlin.",
        "java.",
        "javax."
    )

    // create dependency graph between folders and files
    fun traverse(folder: FileReference, folderPaths: ArrayList<String>, nodes: ArrayList<HashSet<String>>) {
        for (file in folder.listChildren() ?: return) {
            if (file.isDirectory) {
                val filePath = addPrefix(folderPaths.lastOrNull(), "/", file.name)
                folderPaths.add(filePath)
                nodes.add(graph.getOrPut(filePath) { HashSet() })
                traverse(file, folderPaths, nodes)
                nodes.removeLast()
                folderPaths.removeLast()
            } else {
                when (file.lcExtension) {
                    "java", "kt" -> {
                        val lines = file.readLinesSync(Int.MAX_VALUE)
                            .map { it.trim() }
                            .filter { it.startsWith("import ") }
                            .map {
                                var i0 = it.indexOfFirst { c -> c.isUpperCase() }
                                if (i0 < 0) i0 = it.length
                                var i1 = it.lastIndexOf('.', i0)
                                if (i1 < 0) i1 = i0
                                it.substring("import ".length, i1)
                            }
                        for (line in lines) {
                            if (ignoredPaths.none { line.startsWith(it) }) {
                                for (node in nodes) {
                                    node.add(line)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    traverse(source, arrayListOf(), arrayListOf())

    // todo visualize graph?
    // todo find parts, which could be extracted

    for ((nodeName, node) in graph.toSortedMap()) {
        // merge similar paths into [Name1,Name2,Name3]
        val byPrefix = HashMap<String, HashSet<String>>()
        node
            .filter { !it.startsWith(nodeName) }
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
                    if (names.size == 1) "$path.${names.first()}" else "$path.[${
                        names.toSortedSet().joinToString(",")
                    }]"
                }
            }]"
        )
    }

}