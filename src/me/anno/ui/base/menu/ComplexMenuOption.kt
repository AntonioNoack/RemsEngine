package me.anno.ui.base.menu

import me.anno.language.translation.NameDesc

class ComplexMenuOption(
    title: String,
    description: String,
    isEnabled: Boolean,
    val action: () -> Unit
) : ComplexMenuEntry(title, description, isEnabled) {
    constructor(title: NameDesc, action: () -> Unit) :
            this(title.name, title.desc, true, action)

    constructor(title: NameDesc, isEnabled: Boolean, action: () -> Unit) :
            this(title.name, title.desc, isEnabled, action)
}