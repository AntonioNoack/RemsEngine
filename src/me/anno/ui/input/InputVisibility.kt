package me.anno.ui.input

import me.anno.ui.Panel
import org.apache.logging.log4j.LogManager

object InputVisibility {

    private val LOGGER = LogManager.getLogger(InputVisibility::class)
    private val visible = HashSet<String>()

    operator fun get(title: String) =
        if (title.isEmpty()) true
        else title in visible

    fun toggle(visibilityKey: String, panel: Panel?) {
        if (visibilityKey in visible) hide(visibilityKey, panel)
        else show(visibilityKey, panel)
    }

    fun show(visibilityKey: String, panel: Panel?) {
        visible.add(visibilityKey)
        panel?.invalidateLayout()
        LOGGER.info("Showing {}", visibilityKey)
    }

    fun hide(visibilityKey: String, panel: Panel?) {
        visible.remove(visibilityKey)
        panel?.invalidateLayout()
        LOGGER.info("Hiding {}", visibilityKey)
    }
}