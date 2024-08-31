package me.anno.ui.input

import me.anno.ui.Panel
import me.anno.utils.types.Strings.isBlank2
import org.apache.logging.log4j.LogManager

object InputVisibility {

    private val LOGGER = LogManager.getLogger(InputVisibility::class)
    private val visible = HashSet<String>()

    operator fun get(title: String) =
        if (title.isBlank2()) true
        else title in visible

    operator fun set(title: String, value: Boolean) {
        if (!title.isBlank2() && this[title] != value) {
            toggle(title, null)
        }
    }

    fun toggle(visibilityKey: String, panel: Panel?) {
        if (visibilityKey in visible) hide(visibilityKey, panel)
        else show(visibilityKey, panel)
    }

    fun show(visibilityKey: String, panel: Panel?) {
        if (visible.add(visibilityKey)) {
            panel?.invalidateLayout()
            LOGGER.info("Showing {}", visibilityKey)
        }
    }

    fun hide(visibilityKey: String, panel: Panel?) {
        if (visible.remove(visibilityKey)) {
            panel?.invalidateLayout()
            LOGGER.info("Hiding {}", visibilityKey)
        }
    }
}