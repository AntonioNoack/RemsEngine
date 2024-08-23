package me.anno.ui.base.menu

import me.anno.language.translation.NameDesc

class ComplexMenuOption(
    nameDesc: NameDesc,
    isEnabled: Boolean,
    val action: () -> Unit
) : ComplexMenuEntry(nameDesc, isEnabled) {
    constructor(nameDesc: NameDesc, action: () -> Unit) :
            this(nameDesc, true, action)
}