package me.anno.ui.input

import me.anno.ui.Panel

object InputVisibility {

    private val visible = HashSet<String>()

    operator fun get(title: String) =
        if (title.isEmpty()) true
        else title in visible

    fun toggle(visibilityKey: String, panel: Panel?) {
        if (visibilityKey in visible) visible.remove(visibilityKey)
        else visible.add(visibilityKey)
        panel?.invalidateLayout()
    }

    fun show(visibilityKey: String, panel: Panel?) {
        visible.add(visibilityKey)
        panel?.invalidateLayout()
    }

    fun hide(visibilityKey: String, panel: Panel?) {
        visible.remove(visibilityKey)
        panel?.invalidateLayout()
    }

}