package me.anno.ui.editor.files

import me.anno.cache.Promise
import me.anno.engine.Events.addEvent
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.ui.base.Search
import me.anno.utils.Sleep
import me.anno.utils.Threads.runOnNonGFXThread
import java.util.concurrent.atomic.AtomicInteger

object SearchAlgorithm {

    private class ResultSet(
        val self: FileExplorer, val id: Int,
        val newFiles: List<String>, val newSearch: Search,
        val searchDepth: Int,
        val whenDone: () -> Unit
    ) {

        val refreshThreshold = 4
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
            return self.searchTask.id.get() == id
        }

        fun add(file: FileReference?) {
            if (isFirstCall) {
                addEventIfActive {
                    self.removeOldFiles()
                }
                val root = self.folder.getParent()
                if (root != InvalidRef) toBeShown.add(root)
                isFirstCall = false
            }
            if (file != null && self.shouldShowFile(file)) {
                toBeShown.add(file)
            }
            if (file == null || toBeShown.size >= self.content2d.children.size + refreshThreshold) {
                if (file == null || pushed.incrementAndGet() == 1) {
                    pushResults(file == null)
                }
            }
        }

        private val pushed = AtomicInteger()

        private fun pushResults(wasLastFile: Boolean) {
            addEvent {
                pushed.decrementAndGet()
                if (isActive()) {
                    // check if the folder is still the same
                    synchronized(this) {
                        val endIndex = toBeShown.size
                        for (idx in self.content2d.children.size until endIndex) {
                            val fileI = toBeShown[idx]
                            if (idx == self.content2d.children.size) {
                                val isParent = idx == 0 && fileI == self.folder.getParent()
                                self.content2d += self.createEntry(isParent, fileI)
                            }
                        }
                    }
                    if (wasLastFile) {
                        self.lastFiles = newFiles
                        self.lastSearch = newSearch
                        self.lastSearchDepth = searchDepth
                    }
                }
                if (wasLastFile) {
                    whenDone(self, whenDone)
                }
            }
        }

        fun finish() {
            add(null)
        }
    }

    private fun indexRecursively(
        level0: List<FileReference>, searchDepth: Int, newSearch: Search,
        resultSet: ResultSet
    ) {
        val fileLimit = 10_000
        var searchedSize = 0
        val currLevel = ArrayList(level0)
        val nextLevel = ArrayList<FileReference>()
        for (i in 0 until searchDepth) {
            for (j in currLevel.indices) {
                val file = currLevel[j]
                if (file.isHidden) continue
                if (file.isDirectory || when (file.lcExtension) {
                        "zip", "rar", "7z", "s7z", "jar", "tar", "gz", "xz",
                        "bz2", "lz", "lz4", "lzma", "lzo", "z", "zst",
                        "unitypackage" -> Promise.loadSync { file.isSerializedFolder(it) } == true
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
                    if (!resultSet.isActive()) break
                }
            }
            searchedSize += nextLevel.size
            if (searchedSize > fileLimit) break
            if (!resultSet.isActive()) break
            currLevel.clear()
            currLevel.addAll(nextLevel)
            nextLevel.clear()
        }
    }

    fun createResults(self: FileExplorer) {
        self.searchTask.compute { id, whenDone -> createResultsImpl(self, id, whenDone) }
    }

    private fun createResultsImpl(self: FileExplorer, id: Int, whenDone: () -> Unit) {
        val childrenResult = Promise<List<FileReference>>()
        self.folder.listChildren(childrenResult)
        Sleep.waitUntil("SearchAlgorithm", true, {
            childrenResult.hasValue || id != self.searchTask.id.get()
        }) {
            if (id != self.searchTask.id.get()) {
                // calculation after us wants to continue;
                // we shouldn't block it
                whenDone()
            } else {
                val children1 = childrenResult.value
                val children2 = children1?.filter { !it.isHidden } ?: emptyList()
                createResultImpl2(self, children2, id, whenDone)
            }
        }
    }

    private fun createResultImpl2(self: FileExplorer, childFiles: List<FileReference>, id: Int, whenDone: () -> Unit) {

        var value = self.searchBar.value

        var searchDepth = 3
        if (value.startsWith("d=") && value.length > 2 &&
            value[2] in '0'..'9'
        ) {
            searchDepth = value[2] - '0'
            value = value.substring(3)
        }

        val newSearch = Search(value)
        val newFiles = childFiles.map { it.absolutePath }
        val lastSearch = self.lastSearch

        if (self.lastFiles != newFiles ||
            lastSearch == null ||
            self.lastSearchDepth != searchDepth ||
            !lastSearch.containsAllResultsOf(newSearch)
        ) {
            val resultSet = ResultSet(self, id, newFiles, newSearch, searchDepth, whenDone)
            if (newSearch.matchesEverything()) {
                addEverything(resultSet, childFiles)
            } else {
                // this may be expensive
                runOnNonGFXThread("SearchResults") {
                    addSearching(resultSet, childFiles, newSearch)
                }
            }
        } else {
            filterExistingFiles(self, newSearch, whenDone)
        }
    }

    private fun addEverything(resultSet: ResultSet, childFiles: List<FileReference>) {
        for (i in childFiles.indices) {
            resultSet.add(childFiles[i])
        }
        resultSet.finish()
    }

    private fun addSearching(resultSet: ResultSet, childFiles: List<FileReference>, newSearch: Search) {
        for (i in childFiles.indices) {
            val file = childFiles[i]
            if (newSearch.matches(file.name)) {
                resultSet.add(file)
            }
        }
        indexRecursively(childFiles, resultSet.searchDepth, newSearch, resultSet)
        resultSet.finish()
    }

    private fun filterExistingFiles(self: FileExplorer, newSearch: Search, whenDone: () -> Unit) {
        val entries = self.content2d.children
        for (i in entries.indices) {
            val entry = entries[i] as? FileExplorerEntry ?: continue
            entry.isVisible = entry.isParent || newSearch.matches(entry.fileName)
        }
        whenDone(self, whenDone)
    }

    fun whenDone(self: FileExplorer, whenDone: () -> Unit) {

        // reset query time
        self.isValid = 5f

        whenDone()
    }
}