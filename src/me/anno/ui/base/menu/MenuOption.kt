package me.anno.ui.base.menu

import me.anno.input.MouseButton
import me.anno.language.translation.NameDesc

class MenuOption private constructor(val title: String, val description: String, val action: () -> Unit) {

    constructor(title: NameDesc, action: () -> Unit) : this(title.name, title.desc, action)

    fun toComplex(): ComplexMenuOption {
        return ComplexMenuOption(title, description) { button: MouseButton, _: Boolean ->
            if (button.isLeft) {
                action()
                true
            } else false
        }
    }
}