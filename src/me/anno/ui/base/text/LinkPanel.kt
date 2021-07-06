package me.anno.ui.base.text

import me.anno.config.DefaultConfig
import me.anno.gpu.Cursor
import me.anno.input.MouseButton
import me.anno.ui.style.Style
import me.anno.utils.files.OpenInBrowser.openInBrowser
import java.net.URL

open class LinkPanel(val link: URL, style: Style) : TextPanel(link.toString(), style.getChild("link")) {

    override val className get() = "LinkPanel"

    override fun onMouseClicked(x: Float, y: Float, button: MouseButton, long: Boolean) {
        when {
            button.isLeft -> {
                open()
            }
            button.isRight -> {
                // todo options:
                // todo - copy link to clipboard
                // todo - open link in browser
            }
            else -> super.onMouseClicked(x, y, button, long)
        }
    }

    fun open(){
        link.openInBrowser()
    }

    override fun getCursor(): Long? = Cursor.hand

}