package me.anno.ui.input

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
        setValue(visibilityKey, value)
    }

    fun showByDefault(visibilityKey: String) {
        if (visibilityKey.isNotBlank2() && visibilityKey !in visible) {
            setValue(visibilityKey, true)
        }
    }

    fun toggle(visibilityKey: String) {
        setValue(visibilityKey, !this[visibilityKey])
    }

    fun show(visibilityKey: String) {
        setValue(visibilityKey, true)
    }

    fun hide(visibilityKey: String) {
        setValue(visibilityKey, false)
    }

    private fun setValue(visibilityKey: String, isVisible: Boolean) {
        if (visibilityKey.isBlank2()) return
        val wasVisible = visible.put(visibilityKey, isVisible)
        if (wasVisible != isVisible) {
            LOGGER.info(if (isVisible) "Showing {}" else "Hiding {}", visibilityKey)
        }
    }
}