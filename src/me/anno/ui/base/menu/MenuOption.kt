package me.anno.ui.base.menu

import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc

class MenuOption private constructor(val title: String, val description: String, val action: () -> Unit) {

    constructor(title: NameDesc, action: () -> Unit) : this(title.name, title.desc, action)

    fun onClick(button: MouseButton, isLong: Boolean): Boolean {
        return if (button.isLeft && !isLong) {
            action()
            true
        } else false
    }

    fun toComplex(): ComplexMenuOption {
        return ComplexMenuOption(title, description, this::onClick)
    }
}