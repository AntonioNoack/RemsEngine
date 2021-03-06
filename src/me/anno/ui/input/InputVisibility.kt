package me.anno.ui.input

import me.anno.ui.Panel
import me.anno.ui.base.Visibility

object InputVisibility {

    private val visible = HashMap<String, Visibility>()

    operator fun get(title: String) =
        if (title.isEmpty()) Visibility.VISIBLE
        else visible[title] ?: Visibility.GONE

    fun toggle(visibilityKey: String, panel: Panel?) {
        // LOGGER.debug("Toggle $visibilityKey")
        visible[visibilityKey] = Visibility[this[visibilityKey] != Visibility.VISIBLE]
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

    // private val LOGGER = LogManager.getLogger(InputVisibility::class)

}