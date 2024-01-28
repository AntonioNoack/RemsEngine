package me.anno.ui.base.menu

import me.anno.language.translation.NameDesc

class MenuOption(
    val title: String,
    var description: String,
    val action: () -> Unit
) {

    constructor(title: NameDesc, action: () -> Unit) : this(title.name, title.desc, action)

    var isEnabled = true

    fun setEnabled(enabled: Boolean, reason: String = description): MenuOption {
        isEnabled = enabled
        if (!enabled) description = reason
        return this
    }

    fun toComplex(): ComplexMenuOption {
        return ComplexMenuOption(title, description, isEnabled, action)
    }
}