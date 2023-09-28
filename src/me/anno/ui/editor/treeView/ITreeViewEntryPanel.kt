package me.anno.ui.editor.treeView

import me.anno.ui.base.components.Padding

interface ITreeViewEntryPanel {
    fun setEntrySymbol(symbol: String)
    fun setEntryName(name: String)
    fun setEntryTooltip(ttt: String?)
    val padding: Padding
}