package me.anno.ui.editor.files

import me.anno.ui.base.TextPanel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style
import java.io.File

class PathPanel(file: File?, style: Style) : PanelListX(style) {

    var oldFile: File? = null
    var file: File? = file
        set(value) {
            if (value != oldFile) {
                field = value
                update()
            }
        }

    var onChangeListener: ((File?) -> Unit)? = null

    fun update() {
        clear()

        invalidateLayout()

        val file = file
        var name = file?.name ?: "This Computer"
        if (name.isEmpty()) name = file.toString().replace("\\", "")
        if (name.isEmpty()) return

        val panel = TextPanel(name, style)
        panel.setSimpleClickListener { onChangeListener?.invoke(file?.parentFile) }
        panel.setTooltip(file.toString())
        this += panel

        invalidateLayout()

    }

}