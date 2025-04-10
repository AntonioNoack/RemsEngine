package me.anno.ui.editor.files

import me.anno.gpu.Cursor
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.language.translation.Dict
import me.anno.language.translation.NameDesc
import me.anno.ui.Style
import me.anno.ui.base.SpacerPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.image.TextSizedIconPanel
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.base.text.TextPanel
import me.anno.ui.editor.files.FileExplorerIcons.getDefaultIconPath
import me.anno.utils.Color.mixARGB
import me.anno.utils.types.Booleans.toFloat
import me.anno.utils.types.Strings.getImportTypeByExtension

class FavouritePanel(
    val explorer: FileExplorer,
    val file: FileReference,
    val isRootFile: Boolean,
    style: Style
) : PanelListX(style) {

    companion object {
        private val extraXPadding = 4
    }

    val iconPanel: TextSizedIconPanel
    val titlePanel = TextPanel(file.name, style)
    val spacer = SpacerPanel(extraXPadding, 0, style)

    init {
        val importType = getImportTypeByExtension(file.lcExtension)
        val defaultIcon = getDefaultIconPath(false, file, importType)
        iconPanel = TextSizedIconPanel(defaultIcon, style)

        add(spacer)
        add(iconPanel)
        add(titlePanel)

        titlePanel.padding.left += extraXPadding
        titlePanel.padding.right += extraXPadding
    }

    override fun onGotAction(x: Float, y: Float, dx: Float, dy: Float, action: String, isContinuous: Boolean): Boolean {
        when (action) {
            "OpenOptions" -> {
                if (!isRootFile) openMenu(
                    windowStack, listOf(
                        MenuOption(NameDesc(Dict["Remove from favourites", "ui.fileExplorer.removeFromFavourites"])) {
                            Favourites.removeFavouriteFiles(listOf(file))
                        }
                    ))
            }
            else -> return super.onGotAction(x, y, dx, dy, action, isContinuous)
        }
        return true
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        explorer.switchTo(file)
    }

    val normalBackgroundColor = titlePanel.backgroundColor
    val focusBackgroundColor = titlePanel.focusBackgroundColor

    init {
        titlePanel.disableFocusColors()
    }

    override fun getCursor() = Cursor.hand

    override fun onUpdate() {
        val focusFactor = isAnyChildInFocus.toFloat(1f) + isHovered.toFloat(0.5f)
        val bgColor = mixARGB(normalBackgroundColor, focusBackgroundColor, focusFactor)
        backgroundColor = bgColor
        iconPanel.backgroundColor = bgColor
        titlePanel.backgroundColor = bgColor
        titlePanel.focusBackgroundColor = bgColor
        spacer.backgroundColor = bgColor
    }
}