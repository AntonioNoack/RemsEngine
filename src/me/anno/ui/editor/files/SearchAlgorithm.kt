package me.anno.ui.editor.files

import me.anno.engine.Events.addEvent
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReferenceOrTimeout
import me.anno.ui.base.Search
import me.anno.utils.files.Files.listFiles2

object SearchAlgorithm {

    private class ResultSet(
        val self: FileExplorer,
        val calcIndex: Int,
        val newFiles: List<String>,
        val newSearch: Search
    ) {

        val refreshThreshold = 64
        var index = 0
        val toBeShown = ArrayList<FileReference>(refreshThreshold)
        var isFirstCall = true

        private fun addEventIfActive(callback: () -> Unit) {
            addEvent {
                if (isActive()) {
                    callback()
                }
            }
        }

        fun isActive(): Boolean {
            return self.calcIndex == calcIndex
        }

        fun removeOldFiles() {
            addEventIfActive {
                self.removeOldFiles()
                val parent = self.folder.getParent()
                if (self.shouldShowFile(parent)) {
                    // option to go up a folder
                    self.content2d += self.createEntry(true, parent)
                }
            }
        }

        fun add(file: FileReference?) {
            if (isFirstCall) {
                removeOldFiles()
                isFirstCall = false
            }
            if (file != null) {
                toBeShown.add(file)
            }
            if (file == null || toBeShown.size >= index + refreshThreshold) {
                addEventIfActive {
                    // check if the folder is still the same
                    val endIndex = toBeShown.size
                    for (idx in index until endIndex) {
                        val fileI = toBeShown[idx]
                        if (self.shouldShowFile(fileI)) {
                            val entry = self.createEntry(false, fileI)
                            self.content2d += entry
                        }
                    }
                    index = endIndex
                    if (file == null) {
                        self.lastFiles = newFiles
                        self.lastSearch = newSearch
                    }
                }
                beCancellable()
            }
        }

        fun finish() {
            add(null)
        }

        fun beCancellable() {
            Thread.sleep(0)
        }
    }

    private fun indexRecursively(
        level0: List<FileReference>, searchDepth: Int, newSearch: Search,
        resultSet: ResultSet
    ) {
        val fileLimit = 10000
        var searchedSize = 0
        val currLevel = ArrayList(level0)
        val nextLevel = ArrayList<FileReference>()
        for (i in 0 until searchDepth) {
            for (file in currLevel) {
                if (file.isHidden) continue
                if (file.isDirectory || when (file.lcExtension) {
                        "zip", "rar", "7z", "s7z", "jar", "tar", "gz", "xz",
                        "bz2", "lz", "lz4", "lzma", "lzo", "z", "zst",
                        "unitypackage" -> file.isSerializedFolder()
                        else -> false
                    }
                ) {
                    val children = file.listChildren()
                    for (child in children) {
                        if (!child.isHidden && newSearch.matches(child.name)) {
                            resultSet.add(child)
                        }
                    }
                    nextLevel.addAll(children)
                    Thread.sleep(0)
                }
            }
            searchedSize += nextLevel.size
            if (searchedSize > fileLimit) break
            currLevel.clear()
            currLevel.addAll(nextLevel)
            nextLevel.clear()
            resultSet.beCancellable()
        }
    }

    fun createResults(self: FileExplorer) {
        self.searchTask.compute {
            createResultsImpl(self)
        }
    }

    private fun createResultsImpl(self: FileExplorer) {

        val newSearch = Search(self.searchBar.value)

        val directChildren: List<FileReference> = self.folder.listFiles2()
            .filter { !it.isHidden }
        val newFiles = directChildren.map { it.absolutePath }
        val lastSearch = self.lastSearch

        /*if (self.lastFiles != newFiles) {
            LOGGER.info("Files changed from ${self.lastFiles.size} to ${newFiles.size}")
        } else if (lastSearch == null) {
            LOGGER.info("Never searched before")
        } else if (!lastSearch.containsAllResultsOf(newSearch)) {
            LOGGER.info("Search incompatible")
        }*/

        val calcIndex = ++self.calcIndex
        if (self.lastFiles != newFiles || lastSearch == null || !lastSearch.containsAllResultsOf(newSearch)) {

            // when searching something, also include sub-folders up to depth of xyz
            val searchDepth = self.searchDepth

            val resultSet = ResultSet(self, calcIndex, newFiles, newSearch)
            if (newSearch.matchesEverything()) {
                for (file in directChildren) {
                    resultSet.add(file)
                }
            } else {
                for (file in directChildren) {
                    if (newSearch.matches(file.name)) {
                        resultSet.add(file)
                    }
                }
                indexRecursively(directChildren, searchDepth, newSearch, resultSet)
            }

            resultSet.finish()
        } else {
            val entries = self.content2d.children
            for (i in entries.indices) {
                val entry = entries[i] as? FileExplorerEntry ?: continue
                entry.isVisible = entry.isParent || newSearch.matches(getReferenceOrTimeout(entry.path).name)
            }
        }

        // reset query time
        self.isValid = 5f
    }
}