package me.anno.ui.base.menu

import me.anno.input.MouseButton

class ComplexMenuOption(
    val title: String,
    val description: String,
    val isEnabled: Boolean,
    val action: (button: MouseButton, long: Boolean) -> Boolean
)