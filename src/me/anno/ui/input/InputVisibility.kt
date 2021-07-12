package me.anno.ui.input

import me.anno.ui.base.Panel
import me.anno.ui.base.Visibility

object InputVisibility {

    private val visible = HashMap<String, Visibility>()

    operator fun get(title: String) =
        if (title.isEmpty()) Visibility.VISIBLE
        else visible[title] ?: Visibility.GONE

    fun toggle(visibilityKey: String, panel: Panel?) {
        println("toggle $visibilityKey")
        visible[visibilityKey] = if (this[visibilityKey] != Visibility.VISIBLE) Visibility.VISIBLE else Visibility.GONE
        panel?.invalidateLayout()
    }

    fun show(visibilityKey: String, panel: Panel?) {
        visible[visibilityKey] = Visibility.VISIBLE
        panel?.invalidateLayout()
    }

    fun hide(visibilityKey: String, panel: Panel?) {
        visible[visibilityKey] = Visibility.GONE
        panel?.invalidateLayout()
    }

}