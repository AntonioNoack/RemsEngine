package me.anno.ui.editor.files

import me.anno.io.files.FileReference
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.base.text.TextPanel
import me.anno.ui.style.Style

class PathPanel(file: FileReference?, style: Style) : PanelListX(style) {

    var file: FileReference? = file
        set(value) {
            if (field != value) {
                field = value
                update()
            }
        }

    var onChangeListener: ((FileReference?) -> Unit)? = null

    init {
        update()
    }

    fun update() {
        clear()

        invalidateLayout()

        val file = file
        var name = file?.name ?: "This Computer"
        if (name.isEmpty()) name = file.toString().replace("\\", "")
        if (name.isEmpty()) return

        val panel = TextPanel(name, style)
        panel.addLeftClickListener { onChangeListener?.invoke(file?.getParent()) }
        panel.tooltip = file.toString()
        this += panel

        invalidateLayout()

    }

}