package me.anno.ui.base.menu

import me.anno.language.translation.NameDesc

class ComplexMenuGroup(
    nameDesc: NameDesc,
    isEnabled: Boolean,
    val children: List<ComplexMenuEntry>
) : ComplexMenuEntry(nameDesc, isEnabled)