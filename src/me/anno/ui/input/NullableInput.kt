package me.anno.ui.input

import me.anno.ui.base.IconPanel
import me.anno.ui.base.Panel
import me.anno.ui.base.groups.PanelListX
import me.anno.ui.style.Style

class NullableInput(val content: Panel, style: Style) : PanelListX(style) {

    init {
        add(content)
        add(IconPanel("cross", style).apply {
            tooltip = "Toggle Null"
            addLeftClickListener {
                // todo if is null, reset to non-null
                // todo if not null, hide input panel, or gray it out (click = enable it)
            }
        })
    }

}