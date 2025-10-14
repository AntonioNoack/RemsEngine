package me.anno.ui.editor.files

import me.anno.config.DefaultConfig
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.gpu.GFX
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.utils.OS
import me.anno.utils.structures.Collections.filterIsInstance2
import me.anno.utils.structures.lists.Lists.any2

object Favourites {

    private const val CONFIG_KEY = "ui.fileExplorer.favourites"

    fun getDefault(): List<Favourite> {
        return listOf(
            Favourite("Home", OS.home),
            Favourite("Downloads", OS.downloads),
            Favourite("Documents", OS.documents),
            Favourite("Workspace", if (workspace == OS.documents) InvalidRef else workspace),
            Favourite("Pictures", OS.pictures),
            Favourite("Videos", OS.videos),
            Favourite("Music", OS.music)
        ).filter { it.file != InvalidRef }
    }

    fun getFavouriteFiles(): List<Favourite> {
        val favourites = DefaultConfig[CONFIG_KEY] as? List<*>
        if (favourites == null || favourites.any2 { it !is Favourite }) {
            val values = getDefault()
            DefaultConfig[CONFIG_KEY] = values
            return values
        }
        return favourites.filterIsInstance2(Favourite::class)
    }

    fun addFavouriteFiles(list: List<Favourite>) {
        updateFavouriteFiles(list.map { it.file }, list)
    }

    fun removeFavouriteFiles(list: List<Favourite>) {
        updateFavouriteFiles(list.map { it.file }, emptyList())
    }

    fun updateFavouriteFiles(removed: List<FileReference>, added: List<Favourite>) {
        val newFavourites = getFavouriteFiles().filter { it.file !in removed } + added
        DefaultConfig[CONFIG_KEY] = newFavourites
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