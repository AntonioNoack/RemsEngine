package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.io.files.FileRootRef
import me.anno.io.files.InvalidRef
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.utils.OS
import me.anno.utils.files.LocalFile.toGlobalFile

object Favourites {

    private const val configKey = "ui.fileExplorer.favourites"

    fun getDefault(): List<FileReference> {
        return listOf(
            OS.home,
            OS.downloads,
            OS.documents,
            if(workspace == OS.documents) InvalidRef
            else workspace,
            OS.pictures,
            OS.videos,
            OS.music,
            FileRootRef
        ).filter { it != InvalidRef }
    }

    fun format(list: List<FileReference>): String {
        return list.joinToString("|") { it.toLocalPath() }
    }

    fun getFavouriteFiles(): List<FileReference> {
        return DefaultConfig[configKey, format(getDefault())].split("|")
            .map { it.toGlobalFile() }
    }

    fun addFavouriteFiles(list: List<FileReference>) {
        updateFavouriteFiles(list, list)
    }

    fun removeFavouriteFiles(list: List<FileReference>) {
        updateFavouriteFiles(list, emptyList())
    }

    fun updateFavouriteFiles(removed: List<FileReference>, added: List<FileReference>) {
        val newFavourites = getFavouriteFiles().filter { it !in removed } + added
        DefaultConfig[configKey] = format(newFavourites)
        for (window in GFX.windows) {
            for (window1 in window.windowStack) {
                window1.panel.forAllPanels { fe ->
                    if (fe is FileExplorer) {
                        fe.validateFavourites(newFavourites)
                    }
                }
            }
        }
    }

}