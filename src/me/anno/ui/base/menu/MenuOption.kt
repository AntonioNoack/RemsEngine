package me.anno.ui.base.menu

import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc

class MenuOption constructor(
    val title: String,
    var description: String,
    val action: () -> Unit
) {

    constructor(title: NameDesc, action: () -> Unit) : this(title.name, title.desc, action)

    var isEnabled = true

    fun disable(reason: String = description): MenuOption {
        isEnabled = false
        description = reason
        return this
    }

    fun setEnabled(enabled: Boolean, reason: String = description): MenuOption {
        isEnabled = enabled
        if (!enabled) description = reason
        return this
    }

    fun onClick(button: MouseButton, isLong: Boolean): Boolean {
        return if (button.isLeft && !isLong) {
            action()
            true
        } else false
    }

    fun toComplex(): ComplexMenuOption {
        return ComplexMenuOption(title, description, isEnabled, this::onClick)
    }

}