package me.anno.ui.base.menu

import me.anno.input.MouseButton

class ComplexMenuOption(
    title: String,
    description: String,
    isEnabled: Boolean,
    /**
     * action shall return true if the menu is to be closed
     * */
    val action: (button: MouseButton, long: Boolean) -> Boolean
): ComplexMenuEntry(title, description, isEnabled)