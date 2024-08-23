package me.anno.ui.base.menu

import me.anno.language.translation.NameDesc

class MenuOption(
    val nameDesc: NameDesc,
    val action: () -> Unit
) {

    var isEnabled = true
    var description = nameDesc.desc

    fun setEnabled(enabled: Boolean, reason: String = nameDesc.desc): MenuOption {
        isEnabled = enabled
        if (!enabled) description = reason
        return this
    }

    fun toComplex(): ComplexMenuOption {
        return ComplexMenuOption(NameDesc(nameDesc.name, description, ""), isEnabled, action)
    }
}