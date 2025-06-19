package me.anno.ui.editor.files

import me.anno.cache.AsyncCacheData
import me.anno.engine.Events.addEvent
import me.anno.io.files.FileReference
import me.anno.ui.base.Search
import me.anno.utils.assertions.assertTrue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

object SearchAlgorithm {

    private class ResultSet(
        val self: FileExplorer, val id: Int,
        val newFiles: List<String>, val newSearch: Search
    ) {

        val refreshThreshold = 64
        var numShownFiles = 0
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

        fun removeOldFiles() {
            addEventIfActive {
                self.removeOldFiles()
                assertTrue(self.content2d.children.isEmpty())
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
            if (file == null || toBeShown.size >= numShownFiles + refreshThreshold) {
                if (pushed.incrementAndGet() == 1) {
                    pushResults(file == null)
                }
            }
        }

        private val pushed = AtomicInteger()

        fun pushResults(wasLastFile: Boolean) {
            addEventIfActive {
                pushed.decrementAndGet()
                // check if the folder is still the same
                synchronized(this) {
                    val endIndex = toBeShown.size
                    for (idx in numShownFiles until endIndex) {
                        val fileI = toBeShown[idx]
                        if (self.shouldShowFile(fileI)) {
                            val entry = self.createEntry(false, fileI)
                            self.content2d += entry
                        }
                    }
                    numShownFiles = endIndex
                    // sortEntries(self)
                }
                if (wasLastFile) {
                    self.lastFiles = newFiles
                    self.lastSearch = newSearch
                }
            }
        }

        fun finish(whenDone: () -> Unit) {
            add(null)
            addEvent {
                thread(name = "Search-Sorting") {
                    whenDone(self, whenDone)
                }
            }
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
                        "unitypackage" -> AsyncCacheData.loadSync { file.isSerializedFolder(it) } == true
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
        val childrenResult = AsyncCacheData<List<FileReference>>()
        val folder = self.folder
        folder.listChildren(childrenResult)
        childrenResult.waitFor { children ->
            val children2 = children?.filter { !it.isHidden } ?: emptyList()
            createResultImpl2(self, children2, id, whenDone)
        }
    }

    private fun createResultImpl2(self: FileExplorer, childFiles: List<FileReference>, id: Int, whenDone: () -> Unit) {

        val newSearch = Search(self.searchBar.value)
        val newFiles = childFiles.map { it.absolutePath }
        val lastSearch = self.lastSearch

        if (self.lastFiles != newFiles || lastSearch == null || !lastSearch.containsAllResultsOf(newSearch)) {
            val resultSet = ResultSet(self, id, newFiles, newSearch)
            if (newSearch.matchesEverything()) {
                addEverything(resultSet, childFiles, self, whenDone)
            } else {
                // this may be expensive
                thread(name = "SearchResults") {
                    addSearching(resultSet, childFiles, self, newSearch, whenDone)
                }
            }
        } else {
            filterExistingFiles(self, newSearch, whenDone)
        }
    }

    private fun addEverything(
        resultSet: ResultSet, childFiles: List<FileReference>, self: FileExplorer,
        whenDone: () -> Unit
    ) {
        for (i in childFiles.indices) {
            resultSet.add(childFiles[i])
        }
        resultSet.finish(whenDone)
    }

    private fun addSearching(
        resultSet: ResultSet, childFiles: List<FileReference>, self: FileExplorer,
        newSearch: Search, whenDone: () -> Unit
    ) {
        for (i in childFiles.indices) {
            val file = childFiles[i]
            if (newSearch.matches(file.name)) {
                resultSet.add(file)
            }
        }
        indexRecursively(childFiles, self.searchDepth, newSearch, resultSet)
        resultSet.finish(whenDone)
    }

    private fun filterExistingFiles(self: FileExplorer, newSearch: Search, whenDone: () -> Unit) {
        val entries = self.content2d.children
        for (i in entries.indices) {
            val entry = entries[i] as? FileExplorerEntry ?: continue
            entry.isVisible = entry.isParent || newSearch.matches(entry.fileName)
        }
        whenDone(self, whenDone)
    }

    private fun sortEntries(self: FileExplorer) {
        // sorting outside main thread is more performant, but could be dangerous
        self.content2d.children.sortWith(self.sorter)
    }

    fun whenDone(self: FileExplorer, whenDone: () -> Unit) {
        sortEntries(self)

        // reset query time
        self.isValid = 5f

        whenDone()
    }
}