package me.anno.ui.base.text

import me.anno.gpu.Cursor
import me.anno.input.Input.setClipboardContent
import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc
import me.anno.ui.base.menu.Menu.openMenu
import me.anno.ui.base.menu.MenuOption
import me.anno.ui.style.Style
import me.anno.utils.files.OpenInBrowser.openInBrowser
import java.net.URL

open class LinkPanel(link: String, style: Style) : TextPanel(link, style.getChild("link")) {

    constructor(style: Style) : this("", style)

    @Suppress("unused")
    constructor(link: URL, style: Style) : this(link.toString(), style)

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isLeft -> open()
            button.isRight -> {
                openMenu(windowStack, listOf(
                    MenuOption(NameDesc("Copy to Clipboard")) { setClipboardContent(text) },
                    MenuOption(NameDesc("Open in Browser")) { open() }
                ))
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun open() {
        URL(text).openInBrowser()
    }

    override fun getCursor(): Long? = Cursor.hand

    override fun clone(): LinkPanel {
        val clone = LinkPanel(text, style)
        copy(clone)
        return clone
    }

    override val className get() = "LinkPanel"

}