package me.anno.ui.base.menu

import me.anno.input.Key


class ComplexMenuOption(
    title: String,
    description: String,
    isEnabled: Boolean,
    /**
     * action shall return true if the menu is to be closed
     * */
    val action: (button: Key, long: Boolean) -> Boolean
) : ComplexMenuEntry(title, description, isEnabled)