package me.anno.ui.input

import me.anno.ui.Panel
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.isNotBlank2
import org.apache.logging.log4j.LogManager

object InputVisibility {

    private val LOGGER = LogManager.getLogger(InputVisibility::class)
    private val visible = HashMap<String, Boolean>()

    operator fun get(visibilityKey: String) =
        if (visibilityKey.isBlank2()) true
        else visible[visibilityKey] ?: false

    operator fun set(visibilityKey: String, value: Boolean) {
        setValue(visibilityKey, value, null)
    }

    fun showByDefault(visibilityKey: String, panel: Panel?) {
        if (visibilityKey.isNotBlank2() && visibilityKey !in visible) {
            setValue(visibilityKey, true, panel)
        }
    }

    fun toggle(visibilityKey: String, panel: Panel?) {
        setValue(visibilityKey, !this[visibilityKey], panel)
    }

    fun show(visibilityKey: String, panel: Panel?) {
        setValue(visibilityKey, true, panel)
    }

    fun hide(visibilityKey: String, panel: Panel?) {
        setValue(visibilityKey, false, panel)
    }

    private fun setValue(visibilityKey: String, isVisible: Boolean, panel: Panel?) {
        if (visibilityKey.isBlank2()) return
        val wasVisible = visible.put(visibilityKey, isVisible)
        if (wasVisible != isVisible) {
            panel?.invalidateLayout()
            LOGGER.info(if (isVisible) "Showing {}" else "Hiding {}", visibilityKey)
        }
    }
}